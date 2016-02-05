package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 02/02/2016.
 */
public class LNPaymentHelperImpl implements LNPaymentHelper {

    LNOnionHelper onionHelper;
    DBHandler dbHandler;

    List<LNPaymentProcessor> processorList = new ArrayList<>();

    public LNPaymentHelperImpl (LNOnionHelper onionHelper, DBHandler dbHandler) {
        this.onionHelper = onionHelper;
        this.dbHandler = dbHandler;
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
            OnionObject object = paymentData.onionObject;
            onionHelper.loadMessage(object);
        } catch(Exception e) {
            e.printStackTrace();
            processorSent.refundPayment(paymentData);
            return;
        }

        saveReceiverToDatabase(paymentData);

        if (onionHelper.isLastHop()) {
            PaymentSecret secret = dbHandler.getPaymentSecret(paymentData.secret);
            if (secret == null) {
                System.out.println("Can't redeem payment - refund!");
                processorSent.refundPayment(paymentData);
            } else {
                System.out.println("Received money!");
                processorSent.redeemPayment(secret);
            }

        } else {

            ECKey nextHop = onionHelper.getNextHop();
            System.out.println("Next Hop: " + nextHop);
            paymentData.onionObject = onionHelper.getMessageForNextHop();

            //TODO Do the FEE stuff somewhere here maybe?

            for (LNPaymentProcessor processor : processorList) {
                if (processor.connectsToNodeId(nextHop.getPubKey())) {
                    processor.makePayment(paymentData);
                    return;
                }
            }

            //TODO Can't connect the payment right now. Check the DB if we even have a channel with
            //          the next node, refund the payment back if we don't...

            // Will just refund for now...
            System.out.println("Currently not connected with "+Tools.bytesToHex(nextHop.getPubKey())+". Refund...");
            processorSent.refundPayment(paymentData);
        }

    }

    private void saveReceiverToDatabase (PaymentData payment) {
        if (onionHelper.isLastHop()) {
            dbHandler.updatePaymentAddReceiverAddress(payment.secret, new byte[0]);
        } else {
            dbHandler.updatePaymentAddReceiverAddress(payment.secret, onionHelper.getNextHop().getPubKey());
        }
    }

    @Override
    public void paymentRedeemed (PaymentSecret paymentSecret) {
        System.out.println("Payment redeemed: " + paymentSecret);
        byte[] sender = dbHandler.getSenderOfPayment(paymentSecret);

        if (sender == null) {
            System.out.println("Can't resolve the sender of this payment..");
            //TODO ??? - this should not happen - but can't really resolve it either..
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
}
