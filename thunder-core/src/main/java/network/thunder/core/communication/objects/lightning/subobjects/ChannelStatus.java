package network.thunder.core.communication.objects.lightning.subobjects;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 16/12/2015.
 */
public class ChannelStatus implements Cloneable {
    public long amountClient;
    public long amountServer;

    public List<PaymentData> oldPayments = new ArrayList<>();

    public List<PaymentData> newPayments = new ArrayList<>();

    public List<PaymentData> refundedPayments = new ArrayList<>();
    public List<PaymentData> redeemedPayments = new ArrayList<>();

    public int feePerByte;
    public long csvDelay;

    @Override
    protected Object clone () throws CloneNotSupportedException {
        return super.clone();
    }

    public ChannelStatus getClone () {
        try {
            ChannelStatus status = (ChannelStatus) this.clone();
            status.oldPayments = new ArrayList<>(this.oldPayments);
            status.newPayments = new ArrayList<>(this.newPayments);
            status.redeemedPayments = new ArrayList<>(this.redeemedPayments);
            status.refundedPayments = new ArrayList<>(this.refundedPayments);
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
        reverseSending(status.oldPayments);
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
}
