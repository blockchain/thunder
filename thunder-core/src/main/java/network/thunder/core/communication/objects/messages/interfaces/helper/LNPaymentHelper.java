package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.OnionObject;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.processor.interfaces.LNPaymentProcessor;

public interface LNPaymentHelper {
    public void addProcessor(LNPaymentProcessor processor);

    public void removeProcessor(LNPaymentProcessor processor);

    public void relayPayment(PaymentData paymentData, OnionObject onionObject);
}
