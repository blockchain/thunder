package network.thunder.core.communication.processor.implementations.lnpayment.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementPayment extends QueueElement {

    public QueueElementPayment (PaymentData paymentData) {
        this.paymentData = paymentData;
    }

    public PaymentData paymentData;

    @Override
    public ChannelStatus produceNewChannelStatus (ChannelStatus channelStatus) {

        ChannelStatus status = channelStatus.getClone();
        if (channelStatus.amountServer > paymentData.amount) {
            status.newPayments.add(paymentData);
            status.amountServer -= paymentData.amount;
        } else {
            //TODO CANNOT RELAY PAYMENT..
        }

        return status;
    }
}
