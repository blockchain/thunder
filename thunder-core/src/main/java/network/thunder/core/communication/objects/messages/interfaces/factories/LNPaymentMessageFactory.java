package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

/**
 * Created by matsjerratsch on 04/01/2016.
 */
public interface LNPaymentMessageFactory extends MessageFactory {
    LNPaymentAMessage getMessageA (Channel channel, ChannelStatus statusTemp);

    LNPaymentBMessage getMessageB (Channel channel);

    LNPaymentCMessage getMessageC (Channel channel, List<TransactionSignature> channelSignatures, List<TransactionSignature> paymentSignatures);

    LNPaymentDMessage getMessageD (Channel channel);
}
