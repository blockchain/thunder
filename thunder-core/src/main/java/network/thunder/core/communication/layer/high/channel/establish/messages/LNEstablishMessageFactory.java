package network.thunder.core.communication.layer.high.channel.establish.messages;

import network.thunder.core.communication.layer.MessageFactory;
import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

public interface LNEstablishMessageFactory extends MessageFactory {
    LNEstablishAMessage getEstablishMessageA (Channel channel);

    LNEstablishBMessage getEstablishMessageB (TransactionSignature channelSignature);

    LNEstablishCMessage getEstablishMessageC (Transaction transaction);

    LNEstablishDMessage getEstablishMessageD ();
}
