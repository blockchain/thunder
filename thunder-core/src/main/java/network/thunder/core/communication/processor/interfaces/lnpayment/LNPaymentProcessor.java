package network.thunder.core.communication.processor.interfaces.lnpayment;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.Processor;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public abstract class LNPaymentProcessor extends Processor {
    public final static int TIMEOUT_NEGOTIATION = 10 * 1000;

    public abstract boolean connectsToNodeId (byte[] nodeId);

    public abstract byte[] connectsTo ();

    public abstract boolean makePayment (PaymentData paymentData);

    public abstract boolean redeemPayment (PaymentSecret paymentData);

    public abstract boolean refundPayment (PaymentData paymentData);

    public abstract void abortCurrentExchange ();
}
