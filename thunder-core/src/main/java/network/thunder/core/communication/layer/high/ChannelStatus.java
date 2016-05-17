package network.thunder.core.communication.layer.high;

import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import org.bitcoinj.core.Address;

import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChannelStatus implements Cloneable {
    public long amountClient;
    public long amountServer;

    @OneToMany(targetEntity = Channel.class, mappedBy = "hash", fetch = FetchType.EAGER)
    public List<PaymentData> paymentList = new ArrayList<>();

    public int feePerByte;
    public long csvDelay;

    @OneToOne(fetch = FetchType.EAGER)
    public RevocationHash revocationHashClient;
    @OneToOne(fetch = FetchType.EAGER)
    public RevocationHash revocationHashServer;

    @OneToOne(fetch = FetchType.EAGER)
    transient public Address addressClient;
    @OneToOne(fetch = FetchType.EAGER)
    transient public Address addressServer;

    @Override
    protected Object clone () throws CloneNotSupportedException {
        return super.clone();
    }

    @Transient
    public ChannelStatus getClone () {
        try {
            ChannelStatus status = (ChannelStatus) this.clone();
            status.paymentList = clonePaymentList(this.paymentList);
            return status;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Transient
    public ChannelStatus getCloneReversed () {
        ChannelStatus status = getClone();

        long tempAmount = status.amountServer;
        status.amountServer = status.amountClient;
        status.amountClient = tempAmount;

        RevocationHash tempRevocationHash = status.revocationHashServer;
        status.revocationHashServer = status.revocationHashClient;
        status.revocationHashClient = tempRevocationHash;

        Address tempAddress = status.addressServer;
        status.addressServer = status.addressClient;
        status.addressClient = tempAddress;

        reverseSending(status.paymentList);

        return status;
    }

    public void applyUpdate (ChannelUpdate update) {
        for (PaymentData refund : update.refundedPayments) {
            if (refund.sending) {
                amountServer += refund.amount;
            } else {
                amountClient += refund.amount;
            }
        }
        for (PaymentData redeem : update.redeemedPayments) {
            if (redeem.sending) {
                amountClient += redeem.amount;
            } else {
                amountServer += redeem.amount;
            }
        }
        for (PaymentData payment : update.newPayments) {
            if (payment.sending) {
                amountServer -= payment.amount;
            } else {
                amountClient -= payment.amount;
            }
        }

        List<PaymentData> removedPayments = new ArrayList<>();
        removedPayments.addAll(update.redeemedPayments);
        removedPayments.addAll(update.refundedPayments);

        paymentList.addAll(update.newPayments);
        Iterator<PaymentData> iterator = paymentList.iterator();
        while (iterator.hasNext()) {
            PaymentData paymentData = iterator.next();

            if (removedPayments.contains(paymentData)) {
                removedPayments.remove(paymentData);
                iterator.remove();
            }
        }

        this.feePerByte = update.feePerByte;
        this.csvDelay = update.csvDelay;
    }

    private List<PaymentData> reverseSending (List<PaymentData> paymentDataList) {
        for (PaymentData payment : paymentDataList) {
            payment.sending = !payment.sending;
        }
        return paymentDataList;
    }

    private List<PaymentData> clonePaymentList (List<PaymentData> paymentList) {
        List<PaymentData> list = new ArrayList<>(this.paymentList.size());
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
                "amountClient=" + amountClient +
                ", amountServer=" + amountServer +
                ", addressServer=" + addressServer +
                ", addressClient=" + addressClient +
                ", paymentList=" + paymentList.size() +
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
