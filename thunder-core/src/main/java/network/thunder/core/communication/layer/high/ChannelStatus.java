package network.thunder.core.communication.layer.high;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.payments.PaymentData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 16/12/2015.
 */
public class ChannelStatus implements Cloneable {
    public long amountClient;
    public long amountServer;

    public List<PaymentData> remainingPayments = new ArrayList<>();

    public List<PaymentData> newPayments = new ArrayList<>();

    public List<PaymentData> refundedPayments = new ArrayList<>();
    public List<PaymentData> redeemedPayments = new ArrayList<>();

    public int feePerByte;
    public long csvDelay;

    public void applyConfiguration (LNConfiguration configuration) {
        this.feePerByte = configuration.DEFAULT_FEE_PER_BYTE;
        this.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;
    }

    @Override
    protected Object clone () throws CloneNotSupportedException {
        return super.clone();
    }

    public ChannelStatus getClone () {
        try {
            ChannelStatus status = (ChannelStatus) this.clone();
            status.remainingPayments = clonePaymentList(this.remainingPayments);
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

    public ChannelStatus getCloneReversed () {
        ChannelStatus status = getClone();

        long temp = status.amountServer;
        status.amountServer = status.amountClient;
        status.amountClient = temp;

        reverseSending(status.newPayments);
        reverseSending(status.remainingPayments);
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

    private List<PaymentData> clonePaymentList (List<PaymentData> paymentList) {
        List<PaymentData> list = new ArrayList<>(this.remainingPayments.size());
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
        return "ChannelStatus{" +
                ", amountClient=" + amountClient +
                ", amountServer=" + amountServer +
                ", newPayments=" + listToString(newPayments) +
                ", remainingPayments=" + listToString(remainingPayments) +
                ", refundedPayments=" + listToString(refundedPayments) +
                ", redeemedPayments=" + listToString(redeemedPayments) +
                ", csvDelay=" + csvDelay +

                '}';
    }

    private static String listToString (List list) {
        String s = list.size() + " ";
        if (list.size() > 0) {
            for (Object o : list) {
                s += o.toString() + " - ";
            }
        }
        s = s.substring(0, s.length() - 2);
        return s;
    }
}
