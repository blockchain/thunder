package network.thunder.core.communication.layer.high.payments.updates;

import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;

public class PaymentNew {
    public long amount;
    public int timestampRefund;
    public PaymentSecret secret;
    public OnionObject onionObject;

    public PaymentNew copy () {
        PaymentNew p = new PaymentNew();
        p.amount = amount;
        p.secret = secret.copy();
        p.timestampRefund = timestampRefund;
        p.onionObject = new OnionObject(onionObject.data);
        return p;
    }

    @Override
    public String toString () {
        return "PaymentNew{" +
                "amount=" + amount +
                ", timestampRefund=" + timestampRefund +
                ", secret=" + secret +
                '}';
    }
}
