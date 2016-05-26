package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.layer.Processor;

public abstract class LNPaymentProcessor extends Processor {
    public abstract void ping();
}
