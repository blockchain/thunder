package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;

public interface LNPaymentHelper {
    void addProcessor (LNPaymentProcessor processor);

    void removeProcessor (LNPaymentProcessor processor);

    void relayPayment (LNPaymentProcessor paymentProcessor, PaymentData paymentData);

    void makePayment(PaymentData paymentData);

    void paymentRedeemed (PaymentSecret paymentSecret);

    void paymentRefunded (PaymentData paymentSecret);
}
