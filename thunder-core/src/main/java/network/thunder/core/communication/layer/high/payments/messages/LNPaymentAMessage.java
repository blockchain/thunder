package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.RevocationHash;

import java.util.Random;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNPaymentAMessage implements LNPayment {

    public int dice;

    public ChannelStatus channelStatus;
    public RevocationHash newRevocation;

    public LNPaymentAMessage (ChannelStatus channelStatus, RevocationHash newRevocation) {
        this.dice = new Random().nextInt(Integer.MAX_VALUE);

        this.channelStatus = channelStatus;
        this.newRevocation = newRevocation;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(channelStatus);
        Preconditions.checkNotNull(newRevocation);
    }

    @Override
    public String toString () {
        return "LNPaymentAMessage{" +
                "channelStatus=" + channelStatus +
                '}';
    }
}
