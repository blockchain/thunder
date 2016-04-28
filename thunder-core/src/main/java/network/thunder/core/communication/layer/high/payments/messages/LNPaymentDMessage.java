package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.RevocationHash;

import java.util.List;

public class LNPaymentDMessage implements LNPayment {

    public List<RevocationHash> oldRevocationHashes;

    public LNPaymentDMessage (List<RevocationHash> oldRevocationHashes) {
        this.oldRevocationHashes = oldRevocationHashes;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(oldRevocationHashes);
    }

    @Override
    public String toString () {
        return "LNPaymentDMessage{}";
    }
}
