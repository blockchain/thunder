package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.payments.updates.PaymentNew;
import network.thunder.core.communication.layer.high.payments.updates.PaymentRedeem;
import network.thunder.core.communication.layer.high.payments.updates.PaymentRefund;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChannelUpdate implements Cloneable {
    public List<PaymentNew> newPayments = new ArrayList<>();
    public Set<PaymentRefund> refundedPayments = new HashSet<>();
    public Set<PaymentRedeem> redeemedPayments = new HashSet<>();

    public int feePerByte;
    public int csvDelay;

    public void applyConfiguration (LNConfiguration configuration) {
        this.feePerByte = configuration.DEFAULT_FEE_PER_BYTE;
        this.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;
    }

    public ChannelUpdate getClone () {
        try {
            ChannelUpdate status = (ChannelUpdate) this.clone();
            status.newPayments = this.newPayments.stream().map(PaymentNew::copy).collect(Collectors.toList());
            status.redeemedPayments = new HashSet<>(this.redeemedPayments);
            status.refundedPayments = new HashSet<>(this.refundedPayments);

            status.csvDelay = this.csvDelay;
            status.feePerByte = this.feePerByte;

            return status;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Integer> getRemovedPaymentIndexes () {
        List<Integer> payments = new ArrayList<>();
        payments.addAll(refundedPayments.stream().map(r -> r.paymentIndex).collect(Collectors.toList()));
        payments.addAll(redeemedPayments.stream().map(r -> r.paymentIndex).collect(Collectors.toList()));
        return payments;
    }

    @Override
    public String toString () {
        return "ChannelUpdate{" +
                "newPayments=" + newPayments.size() +
                ", refundedPayments=" + refundedPayments.size() +
                ", redeemedPayments=" + redeemedPayments.size() +
                '}';
    }
}
