package network.thunder.core.communication.objects.messages.interfaces.factories;

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
public interface LNEstablishFactory extends MessageFactory {
    LNEstablishAMessage getEstablishMessageA (Channel channel);

    LNEstablishBMessage getEstablishMessageB (Channel channel, Transaction anchor);

    LNEstablishCMessage getEstablishMessageC (Transaction anchor, TransactionSignature escapeSignature, TransactionSignature escapeFastSignature);

    LNEstablishDMessage getEstablishMessageD (TransactionSignature escapeSignature, TransactionSignature escapeFastSignature);
}
