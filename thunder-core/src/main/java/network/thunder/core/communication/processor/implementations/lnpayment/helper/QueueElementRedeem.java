package network.thunder.core.communication.processor.implementations.lnpayment.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementRedeem extends QueueElement {

    PaymentSecret paymentSecret;

    public QueueElementRedeem (PaymentSecret paymentSecret) {
        this.paymentSecret = paymentSecret;
    }

    @Override
    public ChannelStatus produceNewChannelStatus (ChannelStatus channelStatus) {
        ChannelStatus status = channelStatus.getClone();

        PaymentData paymentData = null;
        for (PaymentData p : channelStatus.oldPayments) {
            if (p.secret.equals(paymentSecret)) {
                paymentData = p;
            }
        }

        if (paymentData == null) {
            //TODO We want to redeem a payment, but apparently it's no longer within the oldpayments?
            System.out.println("QueueElementRedeem: Can't redeem, not part of old payments..");
            return channelStatus;
        }

        status.redeemedPayments.add(paymentData);
        status.oldPayments.remove(paymentData);

        status.amountServer += paymentData.amount;

        return status;
    }
}
