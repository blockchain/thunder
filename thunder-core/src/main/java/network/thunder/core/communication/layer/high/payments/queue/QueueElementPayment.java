package network.thunder.core.communication.layer.high.payments.queue;

import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementPayment extends QueueElement {

    public QueueElementPayment (PaymentData paymentData) {
        this.paymentData = paymentData;
    }

    public PaymentData paymentData;

    @Override
    public ChannelUpdate produceNewChannelStatus (ChannelStatus channel, ChannelUpdate channelUpdate, LNPaymentHelper paymentHelper) {
        ChannelStatus status = channel.getClone();
        ChannelUpdate update = channelUpdate.getClone();

        update.newPayments.add(this.paymentData);

        status.applyUpdate(update);

        //TODO consider fees and dust amounts..
        if (status.amountServer > 0) {
            return update;
        } else {
            System.out.println("Payment amount too big - refund..");
            paymentHelper.paymentRefunded(paymentData);
            return channelUpdate;
            //TODO CANNOT RELAY PAYMENT..
        }
    }
}
