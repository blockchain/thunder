package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;
import java.util.stream.Collectors;

public class LNPaymentCMessage implements LNPayment {
    public List<byte[]> channelSignatures;
    public List<byte[]> paymentSignatures;

    public LNPaymentCMessage (ChannelSignatures channelSignatures) {
        this.channelSignatures = channelSignatures.channelSignatures.stream().map(TransactionSignature::encodeToBitcoin).collect(Collectors.toList());
        this.paymentSignatures = channelSignatures.paymentSignatures.stream().map(TransactionSignature::encodeToBitcoin).collect(Collectors.toList());
    }

    public ChannelSignatures getChannelSignatures () {
        ChannelSignatures signatures = new ChannelSignatures();
        signatures.paymentSignatures = paymentSignatures.stream().map(o -> TransactionSignature.decodeFromBitcoin(o, true)).collect(Collectors.toList());
        signatures.channelSignatures = channelSignatures.stream().map(o -> TransactionSignature.decodeFromBitcoin(o, true)).collect(Collectors.toList());

        return signatures;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(channelSignatures);
        Preconditions.checkNotNull(paymentSignatures);
    }

    @Override
    public String toString () {
        return "LNPaymentCMessage{}";
    }
}
