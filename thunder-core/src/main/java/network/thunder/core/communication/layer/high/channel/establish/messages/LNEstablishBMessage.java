package network.thunder.core.communication.layer.high.channel.establish.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.Collections;

public class LNEstablishBMessage extends LNEstablish {
    public byte[] channelSignature;

    public LNEstablishBMessage (TransactionSignature signature) {
        this.channelSignature = signature.encodeToBitcoin();
    }

    @Override
    public Channel saveToChannel (Channel channel) {
        channel.channelSignatures.channelSignatures = Collections.singletonList(TransactionSignature.decodeFromBitcoin(channelSignature, true));
        return channel;
    }

    public TransactionSignature getChannelSignature () {
        return TransactionSignature.decodeFromBitcoin(channelSignature, true);
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(channelSignature);
    }

    @Override
    public String toString () {
        return "LNEstablishBMessage";
    }
}
