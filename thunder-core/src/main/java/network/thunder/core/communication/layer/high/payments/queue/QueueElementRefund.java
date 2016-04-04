package network.thunder.core.communication.layer.high.payments.queue;

import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementRefund extends QueueElement {

    PaymentSecret paymentSecret;

    public QueueElementRefund (PaymentSecret paymentSecret) {
        this.paymentSecret = paymentSecret;
    }

    @Override
    public ChannelUpdate produceNewChannelStatus (ChannelStatus channel, ChannelUpdate channelUpdate, LNPaymentHelper paymentHelper) {
        //TODO Also test yet-to-be-included payments for refund..

        PaymentData paymentRefund = null;
        for (PaymentData payment : channel.paymentList) {
            if (payment.secret.equals(this.paymentSecret)) {
                paymentRefund = payment;
            }
        }
        if (paymentRefund == null) {
            //TODO Payment to be refunded is not in old payments..?
            System.out.println("QueueElementRefund could not find old payment..");
            return channelUpdate;
        }

        //TODO add some disincentives into refunding
        channelUpdate.refundedPayments.add(paymentRefund);
        return channelUpdate;

    }
}
