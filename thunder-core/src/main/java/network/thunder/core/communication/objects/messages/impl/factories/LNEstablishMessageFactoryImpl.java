package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishAMessageImpl;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishBMessageImpl;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishCMessageImpl;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishDMessageImpl;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNEstablishFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types.LNEstablishAMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types.LNEstablishBMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types.LNEstablishCMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types.LNEstablishDMessage;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishMessageFactoryImpl extends MesssageFactoryImpl implements LNEstablishFactory {

    @Override
    public LNEstablishAMessage getEstablishMessageA (Channel channel) {
        LNEstablishAMessage message = new LNEstablishAMessageImpl(
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
        LNEstablishBMessage message = new LNEstablishBMessageImpl(
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
        LNEstablishCMessage message = new LNEstablishCMessageImpl(
                escapeSignature.encodeToBitcoin(),
                escapeFastSignature.encodeToBitcoin(),
                anchor.getHash().getBytes());
        return message;
    }

    @Override
    public LNEstablishDMessage getEstablishMessageD (TransactionSignature escapeSignature, TransactionSignature escapeFastSignature) {
        LNEstablishDMessage message = new LNEstablishDMessageImpl(
                escapeSignature.encodeToBitcoin(),
                escapeFastSignature.encodeToBitcoin()
        );
        return message;
    }

}
