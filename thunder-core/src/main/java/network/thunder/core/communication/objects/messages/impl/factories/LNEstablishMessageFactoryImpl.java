package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNEstablishMessageFactory;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishMessageFactoryImpl extends MesssageFactoryImpl implements LNEstablishMessageFactory {

    @Override
    public LNEstablishAMessage getEstablishMessageA (Channel channel) {
        LNEstablishAMessage message = new LNEstablishAMessage(
                channel.getKeyServer().getPubKey(),
                channel.getKeyServerA().getPubKey(),
                channel.getAnchorSecretHashServer(),
                channel.getAnchorRevocationHashServer(),
                channel.getInitialAmountClient(),
                channel.getInitialAmountServer());
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
                channel.getInitialAmountServer()
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
