package network.thunder.core.communication.objects.messages.impl.message.lnpayment;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;

import java.util.List;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class LNPaymentCMessage implements LNPayment {
    public byte[] newCommitSignature1;
    public byte[] newCommitSignature2;

    public List<byte[]> newPaymentSignatures;

    public LNPaymentCMessage (byte[] newCommitSignature1, byte[] newCommitSignature2, List<byte[]> newPaymentSignatures) {
        this.newCommitSignature1 = newCommitSignature1;
        this.newCommitSignature2 = newCommitSignature2;
        this.newPaymentSignatures = newPaymentSignatures;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(newCommitSignature1);
        Preconditions.checkNotNull(newCommitSignature2);
        Preconditions.checkNotNull(newPaymentSignatures);
    }

    @Override
    public String toString () {
        return "LNPaymentCMessage{}";
    }
}
