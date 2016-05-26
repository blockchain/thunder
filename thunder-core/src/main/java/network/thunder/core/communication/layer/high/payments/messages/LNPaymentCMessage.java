package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.RevocationHash;

public class LNPaymentCMessage extends LNPayment {

    public RevocationHash oldRevocation;
    public RevocationHash newRevocation;

    public LNPaymentCMessage (RevocationHash oldRevocation, RevocationHash newRevocation) {
        this.oldRevocation = oldRevocation;
        this.newRevocation = newRevocation;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(oldRevocation);
        Preconditions.checkNotNull(newRevocation);
    }

    @Override
    public String toString () {
        return "LNPaymentCMessage{}";
    }
}
