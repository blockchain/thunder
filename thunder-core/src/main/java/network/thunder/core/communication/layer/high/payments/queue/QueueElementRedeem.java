package network.thunder.core.communication.layer.high.payments.queue;

import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementRedeem extends QueueElement {

    PaymentSecret paymentSecret;

    public QueueElementRedeem (PaymentSecret paymentSecret) {
        this.paymentSecret = paymentSecret;
    }

    @Override
    public ChannelUpdate produceNewChannelStatus (ChannelStatus channel, ChannelUpdate channelUpdate, LNPaymentHelper paymentHelper) {
        //TODO Also test yet-to-be-included payments for refund..

        ChannelStatus status = channel.getClone();

        PaymentData paymentData = null;
        for (PaymentData p : channel.paymentList) {
            if (p.secret.equals(paymentSecret)) {
                paymentData = p;
            }
        }

        if (paymentData == null) {
            //TODO We want to redeem a payment, but apparently it's no longer within the oldpayments?
            System.out.println("QueueElementRedeem: Can't redeem, not part of old payments..");
            return channelUpdate;
        }

        paymentData.secret.secret = paymentSecret.secret;
        channelUpdate.redeemedPayments.add(paymentData);
        return channelUpdate;
    }
}
