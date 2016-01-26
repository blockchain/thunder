package network.thunder.core.communication.processor.interfaces.lnpayment;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.processor.Processor;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public interface LNPaymentProcessor extends Processor {
    public final static int TIMEOUT_NEGOTIATION = 5 * 1000;

    public boolean connectsToNodeId (byte[] nodeId);

    public boolean makePayment (PaymentData paymentData, OnionObject onionObject);

    public boolean redeemPayment (PaymentData paymentData);

    public boolean refundPayment (PaymentData paymentData);

    public void abortCurrentExchange ();
}
