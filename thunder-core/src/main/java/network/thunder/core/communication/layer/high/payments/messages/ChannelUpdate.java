package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.payments.PaymentData;

import java.util.ArrayList;
import java.util.List;

public class ChannelUpdate implements Cloneable {
    public List<PaymentData> newPayments = new ArrayList<>();
    public List<PaymentData> refundedPayments = new ArrayList<>();
    public List<PaymentData> redeemedPayments = new ArrayList<>();

    public int feePerByte;
    public int csvDelay;

    public void applyConfiguration (LNConfiguration configuration) {
        this.feePerByte = configuration.DEFAULT_FEE_PER_BYTE;
        this.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;
    }

    public ChannelUpdate getClone () {
        try {
            ChannelUpdate status = (ChannelUpdate) this.clone();
            status.newPayments = clonePaymentList(this.newPayments);
            status.redeemedPayments = clonePaymentList(this.redeemedPayments);
            status.refundedPayments = clonePaymentList(this.refundedPayments);

            status.csvDelay = this.csvDelay;
            status.feePerByte = this.feePerByte;

            return status;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<PaymentData> getRemovedPayments () {
        List<PaymentData> payments = new ArrayList<>();
        payments.addAll(redeemedPayments);
        payments.addAll(refundedPayments);
        return payments;
    }

    public ChannelUpdate getCloneReversed () {
        ChannelUpdate status = getClone();

        reverseSending(status.newPayments);
        reverseSending(status.redeemedPayments);
        reverseSending(status.refundedPayments);

        return status;
    }

    private List<PaymentData> reverseSending (List<PaymentData> paymentDataList) {
        for (PaymentData payment : paymentDataList) {
            payment.sending = !payment.sending;
        }
        return paymentDataList;
    }

    private List<PaymentData> clonePaymentList (Iterable<PaymentData> paymentList) {
        List<PaymentData> list = new ArrayList<>();
        for (PaymentData data : paymentList) {
            try {
                list.add((PaymentData) data.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
        return list;
    }

    @Override
    public String toString () {
        return "ChannelUpdate{" +
                "newPayments=" + newPayments +
                ", refundedPayments=" + refundedPayments +
                ", redeemedPayments=" + redeemedPayments +
                ", feePerByte=" + feePerByte +
                ", csvDelay=" + csvDelay +
                '}';
    }
}
