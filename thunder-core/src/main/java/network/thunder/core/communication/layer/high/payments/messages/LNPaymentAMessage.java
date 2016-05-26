package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class LNPaymentAMessage extends LNPayment {

    public int dice;
    public ChannelUpdate channelUpdate;

    public List<byte[]> channelSignatures = new ArrayList<>();
    public List<byte[]> paymentSignatures = new ArrayList<>();

    public LNPaymentAMessage (ChannelUpdate channelUpdate, ChannelSignatures channelSignatures) {
        this.dice = new Random().nextInt(Integer.MAX_VALUE);
        this.channelUpdate = channelUpdate;

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
        Preconditions.checkNotNull(channelUpdate);
        Preconditions.checkNotNull(channelSignatures);
        Preconditions.checkNotNull(paymentSignatures);
    }

    @Override
    public String toString () {
        return "LNPaymentAMessage{dice=" + dice + ", " +
                "channelUpdate="
                + (channelUpdate.newPayments.size() + " " +
                channelUpdate.redeemedPayments.size() + " " +
                channelUpdate.refundedPayments.size()) +
                '}';
    }
}
