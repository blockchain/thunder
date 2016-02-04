package network.thunder.core.communication.processor.implementations.lnpayment.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementRefund extends QueueElement {

    PaymentSecret paymentSecret;

    public QueueElementRefund (PaymentSecret paymentSecret) {
        this.paymentSecret = paymentSecret;
    }

    @Override
    public ChannelStatus produceNewChannelStatus (ChannelStatus channelStatus) {
        ChannelStatus status = channelStatus.getClone();
        PaymentData paymentRefund = null;
        for (PaymentData payment : status.oldPayments) {
            if (payment.secret.equals(this.paymentSecret)) {
                paymentRefund = payment;
            }
        }
        if (paymentRefund == null) {
            //TODO Payment to be refunded is not in old payments..?
            System.out.println("QueueElementRefund could not find old payment..");
            return status;
        } else {
            status.refundedPayments.add(paymentRefund);
            status.oldPayments.remove(paymentRefund);
            //TODO add some disincentives into refunding

            status.amountClient += paymentRefund.amount;
            return status;
        }

    }
}
