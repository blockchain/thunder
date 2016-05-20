package network.thunder.core.communication.layer.high.channel.establish.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Transaction;

public class LNEstablishCMessage implements LNEstablish {
    public byte[] anchorSigned;

    public LNEstablishCMessage (Transaction transaction) {
        this.anchorSigned = transaction.bitcoinSerialize();
    }

    @Override
    public Channel saveToChannel (Channel channel) {
        channel.anchorTx = new Transaction(Constants.getNetwork(), anchorSigned);
        return channel;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(anchorSigned);
    }

    @Override
    public String toString () {
        return "LNEstablishCMessage";
    }
}
