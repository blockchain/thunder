package network.thunder.core.communication.objects.messages.impl.message.lnpayment;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.lightning.RevocationHash;

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
