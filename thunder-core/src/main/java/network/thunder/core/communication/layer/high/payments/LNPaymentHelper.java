package network.thunder.core.communication.layer.high.payments;

public interface LNPaymentHelper {
    void addProcessor (LNPaymentProcessor processor);

    void removeProcessor (LNPaymentProcessor processor);

    void relayPayment (LNPaymentProcessor paymentProcessor, PaymentData paymentData);

    void makePayment (PaymentData paymentData);

    void paymentRedeemed (PaymentSecret paymentSecret);

    void paymentRefunded (PaymentData paymentSecret);
}
