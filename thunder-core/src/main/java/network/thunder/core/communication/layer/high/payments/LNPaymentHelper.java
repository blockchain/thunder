package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.NodeKey;

public interface LNPaymentHelper {
    void addProcessor (NodeKey nodeKey, LNPaymentProcessor processor);

    void removeProcessor (NodeKey nodeKey);

    void makePayment (PaymentData paymentData);
}
