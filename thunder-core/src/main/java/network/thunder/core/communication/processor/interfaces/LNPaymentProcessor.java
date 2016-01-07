package network.thunder.core.communication.processor.interfaces;

import network.thunder.core.communication.objects.OnionObject;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.processor.Processor;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public interface LNPaymentProcessor extends Processor {
    public boolean connectsToNodeId (byte[] nodeId);

    public void makePayment (PaymentData paymentData, OnionObject onionObject);

    public void redeemPayment (PaymentData paymentData);

    public void refundPayment (PaymentData paymentData);
}
