package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import org.bitcoinj.core.ECKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 02/02/2016.
 */
public class LNPaymentHelperImpl implements LNPaymentHelper {

    List<LNPaymentProcessor> processorList = new ArrayList<>();
    DBHandler dbHandler;
    LNOnionHelper onionHelper;

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
        OnionObject object = paymentData.onionObject;
        onionHelper.loadMessage(object);

        if (onionHelper.isLastHop()) {
            //TODO WOO WE GOT MONEY - REDEEM IT IF POSSIBLE
        } else {

            ECKey nextHop = onionHelper.getNextHop();
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
            processorSent.refundPayment(paymentData);
        }

    }

    @Override
    public void paymentRedeemed (PaymentSecret paymentSecret) {
        byte[] sender = dbHandler.getSenderOfPayment(paymentSecret);

        if (sender == null) {
            //TODO ??? - this should not happen - but can't really resolve it either..
        }

        for (LNPaymentProcessor processor : processorList) {
            if (processor.connectsToNodeId(sender)) {
                processor.redeemPayment(paymentSecret);
                return;
            }
        }

        //TODO sender of payment is offline right now - have to close channel if he does not come back online in time..

    }

    @Override
    public void paymentRefunded (PaymentData paymentData) {

        byte[] sender = dbHandler.getSenderOfPayment(paymentData.secret);

        if (sender == null) {
            //TODO ??? - this should not happen - but can't really resolve it either..
        }

        for (LNPaymentProcessor processor : processorList) {
            if (processor.connectsToNodeId(sender)) {
                processor.refundPayment(paymentData);
                return;
            }
        }

        //TODO sender of payment is offline right now - he will close the channel if we can't get back to him in time..

    }
}
