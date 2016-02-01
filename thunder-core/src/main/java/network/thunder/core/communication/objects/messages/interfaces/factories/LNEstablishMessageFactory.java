package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishDMessage;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public interface LNEstablishMessageFactory extends MessageFactory {
    LNEstablishAMessage getEstablishMessageA (Channel channel);

    LNEstablishBMessage getEstablishMessageB (Channel channel, Transaction anchor);

    LNEstablishCMessage getEstablishMessageC (Transaction anchor, TransactionSignature escapeSignature, TransactionSignature escapeFastSignature);

    LNEstablishDMessage getEstablishMessageD (TransactionSignature escapeSignature, TransactionSignature escapeFastSignature);
}
