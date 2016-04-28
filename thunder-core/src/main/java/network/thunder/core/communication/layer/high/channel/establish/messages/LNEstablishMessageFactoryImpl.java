package network.thunder.core.communication.layer.high.channel.establish.messages;

import network.thunder.core.communication.layer.MesssageFactoryImpl;
import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

public class LNEstablishMessageFactoryImpl extends MesssageFactoryImpl implements LNEstablishMessageFactory {

    @Override
    public LNEstablishAMessage getEstablishMessageA (Channel channel) {
        LNEstablishAMessage message = new LNEstablishAMessage(
                channel.getKeyServer().getPubKey(),
                channel.getKeyServerA().getPubKey(),
                channel.getAnchorSecretHashServer(),
                channel.getAnchorRevocationHashServer(),
                channel.getInitialAmountClient(),
                channel.getInitialAmountServer(),
                channel.addressServer);
        return message;
    }

    @Override
    public LNEstablishBMessage getEstablishMessageB (Channel channel, Transaction anchor) {
        LNEstablishBMessage message = new LNEstablishBMessage(
                channel.getKeyServer().getPubKey(),
                channel.getKeyServerA().getPubKey(),
                channel.getAnchorSecretHashServer(),
                channel.getAnchorRevocationHashServer(),
                anchor.getHash().getBytes(),
                channel.getInitialAmountServer(),
                channel.addressServer
        );

        return message;
    }

    @Override
    public LNEstablishCMessage getEstablishMessageC (Transaction anchor, TransactionSignature escapeSignature, TransactionSignature escapeFastSignature) {
        LNEstablishCMessage message = new LNEstablishCMessage(
                escapeSignature.encodeToBitcoin(),
                escapeFastSignature.encodeToBitcoin(),
                anchor.getHash().getBytes());
        return message;
    }

    @Override
    public LNEstablishDMessage getEstablishMessageD (TransactionSignature escapeSignature, TransactionSignature escapeFastSignature) {
        LNEstablishDMessage message = new LNEstablishDMessage(
                escapeSignature.encodeToBitcoin(),
                escapeFastSignature.encodeToBitcoin()
        );
        return message;
    }

}
