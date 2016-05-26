package network.thunder.core.etc;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.LNPaymentProcessor;
import network.thunder.core.communication.layer.high.payments.PaymentData;

public class MockLNPaymentHelper implements LNPaymentHelper {

    @Override
    public void addProcessor (NodeKey nodeKey, LNPaymentProcessor processor) {

    }

    @Override
    public void removeProcessor (NodeKey nodeKey) {

    }

    @Override
    public void makePayment (PaymentData paymentData) {

    }

}
