package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.RevocationHash;

import java.util.List;

public class LNPaymentCMessage extends LNPayment implements LNRevokeOldMessage {

    public List<RevocationHash> oldRevocation;

    @Override
    public List<RevocationHash> getOldRevocationHash () {
        return oldRevocation;
    }

    @Override
    public void setOldRevocationHash (List<RevocationHash> oldRevocationHash) {
        this.oldRevocation = oldRevocationHash;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(oldRevocation);
    }

    @Override
    public String toString () {
        return "LNPaymentCMessage{}";
    }
}
