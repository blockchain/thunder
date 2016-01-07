package network.thunder.core.communication.objects.lightning.subobjects;

import network.thunder.core.communication.objects.OnionObject;

import java.util.List;

/**
 * Created by matsjerratsch on 16/12/2015.
 */
public class ChannelStatus {
    public long amountClient;
    public long amountServer;

    public List<PaymentData> oldPayments;

    public List<PaymentData> newPayments;
    public List<OnionObject> newPaymentsOnionObjects;

    public List<PaymentData> refundedPayments;
    public List<PaymentData> redeemedPayments;

    public int feePerByte;
    public long csvDelay;

}
