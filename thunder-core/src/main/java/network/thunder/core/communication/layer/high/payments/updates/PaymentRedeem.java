package network.thunder.core.communication.layer.high.payments.updates;

import network.thunder.core.communication.layer.high.payments.PaymentSecret;

public class PaymentRedeem {
    public final int paymentIndex;
    public final PaymentSecret secret;

    public PaymentRedeem (PaymentSecret secret, int paymentIndex) {
        this.secret = secret;
        this.paymentIndex = paymentIndex;
    }

    @Override
    public String toString () {
        return "PaymentRedeem{" +
                "paymentIndex=" + paymentIndex +
                ", secret=" + secret +
                '}';
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PaymentRedeem that = (PaymentRedeem) o;

        return paymentIndex == that.paymentIndex;

    }

    @Override
    public int hashCode () {
        return paymentIndex;
    }
}
