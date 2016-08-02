package network.thunder.core.communication.layer.high.payments.updates;

public class PaymentRefund {
    public final int paymentIndex;

    public PaymentRefund (int paymentIndex) {
        this.paymentIndex = paymentIndex;
    }

    @Override
    public String toString () {
        return "PaymentRefund{" +
                "paymentIndex=" + paymentIndex +
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

        PaymentRefund that = (PaymentRefund) o;

        return paymentIndex == that.paymentIndex;

    }

    @Override
    public int hashCode () {
        return paymentIndex;
    }
}
