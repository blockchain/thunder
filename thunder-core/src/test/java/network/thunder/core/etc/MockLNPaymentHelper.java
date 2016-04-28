package network.thunder.core.etc;

import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.LNPaymentProcessor;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;

public class MockLNPaymentHelper implements LNPaymentHelper {
    @Override
    public void addProcessor (LNPaymentProcessor processor) {

    }

    @Override
    public void removeProcessor (LNPaymentProcessor processor) {

    }

    @Override
    public void relayPayment (LNPaymentProcessor paymentProcessor, PaymentData paymentData) {

    }

    @Override
    public void makePayment (PaymentData paymentData) {

    }

    @Override
    public void paymentRedeemed (PaymentSecret paymentSecret) {

    }

    @Override
    public void paymentRefunded (PaymentData paymentSecret) {

    }
}
