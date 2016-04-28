package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.RevocationHash;

public class LNPaymentBMessage implements LNPayment {

    public RevocationHash newRevocation;

    public LNPaymentBMessage (RevocationHash newRevocation) {
        this.newRevocation = newRevocation;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(newRevocation);
    }
}
