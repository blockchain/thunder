package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.layer.MessageFactory;
import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

public interface LNPaymentMessageFactory extends MessageFactory {
    LNPaymentAMessage getMessageA (Channel channel, ChannelUpdate update);

    LNPaymentBMessage getMessageB (Channel channel);

    LNPaymentCMessage getMessageC (Channel channel, List<TransactionSignature> channelSignatures, List<TransactionSignature> paymentSignatures);

    LNPaymentDMessage getMessageD (Channel channel);
}
