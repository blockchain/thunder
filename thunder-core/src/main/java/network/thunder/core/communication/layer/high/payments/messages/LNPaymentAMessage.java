package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.RevocationHash;

import java.security.SecureRandom;

public class LNPaymentAMessage implements LNPayment {

    public int dice;
    public ChannelUpdate channelStatus;
    public RevocationHash newRevocation;

    public LNPaymentAMessage (ChannelUpdate channelUpdate, RevocationHash newRevocation) {
        this.dice = new SecureRandom().nextInt(Integer.MAX_VALUE);

        this.channelStatus = channelUpdate;
        this.newRevocation = newRevocation;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(channelStatus);
        Preconditions.checkNotNull(newRevocation);
    }

    @Override
    public String toString () {
        return "LNPaymentAMessage{dice=" + dice + ", " +
                "channelUpdate=" + (channelStatus.newPayments.size() + channelStatus.redeemedPayments.size() + channelStatus.refundedPayments.size()) +
                '}';
    }
}
