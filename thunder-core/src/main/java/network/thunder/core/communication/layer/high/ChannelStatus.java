package network.thunder.core.communication.layer.high;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import org.bitcoinj.core.Address;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChannelStatus {
    public long amountClient;
    public long amountServer;

    public List<PaymentData> paymentList = new ArrayList<>();

    public int feePerByte;
    public int csvDelay;

    //Various revocation hashes are stored here. They get swapped downwards after an exchange (Next->Current; NextNext->Next)
    //Current revocation hash is the one that we have a current valid channel transaction with
    public RevocationHash revoHashClientCurrent;
    public RevocationHash revoHashServerCurrent;

    //Next revocation hash is the hash used when creating a new channel transaction
    public RevocationHash revoHashClientNext;
    public RevocationHash revoHashServerNext;

    //NextNext is the new hash exchanged on the begin of an exchange
    //For now there is no need to store it in the database
    transient public RevocationHash revoHashClientNextNext;
    transient public RevocationHash revoHashServerNextNext;

    public Address addressClient;
    public Address addressServer;

    public ChannelStatus copy () {
        ChannelStatus status = new ChannelStatus();
        status.amountClient = this.amountClient;
        status.amountServer = this.amountServer;
        status.feePerByte = this.feePerByte;
        status.csvDelay = this.csvDelay;
        status.addressClient = this.addressClient;
        status.addressServer = this.addressServer;
        status.revoHashClientCurrent = revoHashClientCurrent == null ? null : revoHashClientCurrent.copy();
        status.revoHashClientNext = revoHashClientNext == null ? null : revoHashClientNext.copy();
        status.revoHashClientNextNext = revoHashClientNextNext == null ? null : revoHashClientNextNext.copy();
        status.revoHashServerCurrent = revoHashServerCurrent == null ? null : revoHashServerCurrent.copy();
        status.revoHashServerNext = revoHashServerNext == null ? null : revoHashServerNext.copy();
        status.revoHashServerNextNext = revoHashServerNextNext == null ? null : revoHashServerNextNext.copy();

        status.paymentList = clonePaymentList(this.paymentList);
        return status;
    }

    public ChannelStatus reverse () {
        ChannelStatus status = copy();

        long tempAmount = status.amountServer;
        status.amountServer = status.amountClient;
        status.amountClient = tempAmount;

        RevocationHash tempRevocationHash = status.revoHashServerCurrent;
        status.revoHashServerCurrent = status.revoHashClientCurrent;
        status.revoHashClientCurrent = tempRevocationHash;

        RevocationHash tempRevocationHashNext = status.revoHashServerNext;
        status.revoHashServerNext = status.revoHashClientNext;
        status.revoHashClientNext = tempRevocationHashNext;

        RevocationHash tempRevocationHashNextNext = status.revoHashServerNextNext;
        status.revoHashServerNextNext = status.revoHashClientNextNext;
        status.revoHashClientNextNext = tempRevocationHashNextNext;

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

    public void applyNextRevoHash () {
        Preconditions.checkNotNull(this.revoHashClientNext);
        Preconditions.checkNotNull(this.revoHashServerNext);

        this.revoHashClientCurrent = this.revoHashClientNext;
        this.revoHashServerCurrent = this.revoHashServerNext;

        this.revoHashClientNext = null;
        this.revoHashServerNext = null;
    }

    public void applyNextNextRevoHash () {
        Preconditions.checkNotNull(this.revoHashClientNextNext);
        Preconditions.checkNotNull(this.revoHashServerNextNext);

        this.revoHashClientNext = this.revoHashClientNextNext;
        this.revoHashServerNext = this.revoHashServerNextNext;

        this.revoHashClientNextNext = null;
        this.revoHashServerNextNext = null;
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
                ", revoServer=" + revoHashServerCurrent +
                ", revoClient=" + revoHashClientCurrent +
                ", revoServerNext=" + revoHashServerNext +
                ", revoClientNext=" + revoHashClientNext +
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
