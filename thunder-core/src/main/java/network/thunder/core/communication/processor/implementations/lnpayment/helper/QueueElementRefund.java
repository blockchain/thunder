package network.thunder.core.communication.processor.implementations.lnpayment.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementRefund extends QueueElement {

    PaymentData paymentData;

    @Override
    public ChannelStatus produceNewChannelStatus (ChannelStatus channelStatus) {
        ChannelStatus status = channelStatus.getClone();
        status.refundedPayments.add(paymentData);
        status.oldPayments.remove(paymentData);
        //TODO add some disincentives into refunding

        status.amountClient += paymentData.amount;
        return status;
    }
}
