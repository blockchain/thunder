package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;

public interface LNPaymentHelper {
    public void addProcessor (LNPaymentProcessor processor);

    public void removeProcessor (LNPaymentProcessor processor);

    public void relayPayment (LNPaymentProcessor paymentProcessor, PaymentData paymentData);

    public void paymentRedeemed (PaymentSecret paymentSecret);

    public void paymentRefunded (PaymentData paymentSecret);
}
