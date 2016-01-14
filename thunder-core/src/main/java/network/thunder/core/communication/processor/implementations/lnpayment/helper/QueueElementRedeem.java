package network.thunder.core.communication.processor.implementations.lnpayment.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementRedeem extends QueueElement {

    PaymentData paymentData;

    @Override
    public ChannelStatus produceNewChannelStatus (ChannelStatus channelStatus) {
        ChannelStatus status = channelStatus.getClone();
        status.redeemedPayments.add(paymentData);
        status.oldPayments.remove(paymentData);


        status.amountServer += paymentData.amount;

        return status;
    }
}
