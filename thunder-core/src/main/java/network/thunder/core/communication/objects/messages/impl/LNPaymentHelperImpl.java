package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.PeeledOnion;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.ECKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 02/02/2016.
 */
public class LNPaymentHelperImpl implements LNPaymentHelper {

    LNOnionHelper onionHelper;
    DBHandler dbHandler;
    LNEventHelper eventHelper;
    NodeServer nodeServer;

    List<LNPaymentProcessor> processorList = new ArrayList<>();

    public LNPaymentHelperImpl (ContextFactory contextFactory, DBHandler dbHandler) {
        this.onionHelper = contextFactory.getOnionHelper();
        this.dbHandler = dbHandler;
        this.eventHelper = contextFactory.getEventHelper();
        this.nodeServer = contextFactory.getServerSettings();
    }

    @Override
    public void addProcessor (LNPaymentProcessor processor) {
        processorList.add(processor);
    }

    @Override
    public void removeProcessor (LNPaymentProcessor processor) {
        processorList.remove(processor);
    }

    @Override
    public synchronized void relayPayment (LNPaymentProcessor processorSent, PaymentData paymentData) {
        try {

            PeeledOnion peeledOnion = getPeeledOnion(paymentData);
            saveReceiverToDatabase(paymentData, peeledOnion);

            if (peeledOnion.isLastHop) {
                PaymentSecret secret = dbHandler.getPaymentSecret(paymentData.secret);
                if (secret == null) {
                    System.out.println("Can't redeem payment - refund!");
                    processorSent.refundPayment(paymentData);
                } else {
                    System.out.println("Received money!");
                    eventHelper.onPaymentCompleted(secret);
                    processorSent.redeemPayment(secret);
                }

            } else {
                paymentData.onionObject = peeledOnion.onionObject;
                ECKey nextHop = peeledOnion.nextHop;

                if (!relayPaymentToCorrectProcessor(paymentData, nextHop)) {
                    //TODO Can't connect the payment right now. Check the DB if we even have a channel with
                    //          the next node, refund the payment back if we don't...
                    System.out.println("Currently not connected with " + Tools.bytesToHex(nextHop.getPubKey()) + ". Refund...");
                    processorSent.refundPayment(paymentData);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            processorSent.refundPayment(paymentData);
        }

    }

    @Override
    public void makePayment (PaymentData paymentData) {
        try {
            PeeledOnion peeledOnion = getPeeledOnion(paymentData);
            saveReceiverToDatabase(paymentData, peeledOnion);

            paymentData.onionObject = peeledOnion.onionObject;
            ECKey nextHop = peeledOnion.nextHop;

            if (!relayPaymentToCorrectProcessor(paymentData, nextHop)) {
                throw new LNPaymentException("Not connected to next hop " + nextHop);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new LNPaymentException(e);
        }
    }

    private boolean relayPaymentToCorrectProcessor (PaymentData paymentData, ECKey nextHop) {
        System.out.println("Next Hop: " + nextHop);

        for (LNPaymentProcessor processor : processorList) {
            if (processor.connectsToNodeId(nextHop.getPubKey())) {
                PaymentData copy = paymentData.cloneObject();
                copy.sending = true;
                processor.makePayment(copy);
                return true;
            }
        }
        return false;
    }

    private PeeledOnion getPeeledOnion (PaymentData paymentData) {
        return onionHelper.loadMessage(nodeServer.pubKeyServer, paymentData.onionObject);
    }

    private void saveReceiverToDatabase (PaymentData payment, PeeledOnion peeledOnion) {
        if (peeledOnion.isLastHop) {
            dbHandler.updatePaymentAddReceiverAddress(payment.secret, new byte[0]);
        } else {
            dbHandler.updatePaymentAddReceiverAddress(payment.secret, peeledOnion.nextHop.getPubKey());
        }
    }

    @Override
    public void paymentRedeemed (PaymentSecret paymentSecret) {
        System.out.println("Payment redeemed: " + paymentSecret);
        byte[] sender = dbHandler.getSenderOfPayment(paymentSecret);

        if (isEmptyByte(sender)) {
            System.out.println("Payment was redeemed: " + paymentSecret);
            eventHelper.onPaymentCompleted(paymentSecret);
            return;
        } else {

            System.out.println("Sender of payment: " + Tools.bytesToHex(sender));

            for (LNPaymentProcessor processor : processorList) {
                if (processor.connectsToNodeId(sender)) {
                    processor.redeemPayment(paymentSecret);
                    return;
                }
            }
            System.out.println("Aren't connected to redeem payment..");
            //TODO sender of payment is offline right now - have to close channel if he does not come back online in time..
            //TODO redeem payment from other party on blockchain if channel is closed already..
        }
    }

    @Override
    public void paymentRefunded (PaymentData paymentData) {

        byte[] sender = dbHandler.getSenderOfPayment(paymentData.secret);

        if (sender == null) {
            System.out.println("Can't find sender when trying to refund..?");
            //TODO ??? - this should not happen - but can't really resolve it either..
        }

        for (LNPaymentProcessor processor : processorList) {
            if (processor.connectsToNodeId(sender)) {
                System.out.println("refundPayment...");
                processor.refundPayment(paymentData);
                return;
            }
        }

        //TODO sender of payment is offline right now - he will close the channel if we can't get back to him in time..

    }

    private static boolean isEmptyByte (byte[] bytes) {
        return bytes.length == 0;
    }
}
