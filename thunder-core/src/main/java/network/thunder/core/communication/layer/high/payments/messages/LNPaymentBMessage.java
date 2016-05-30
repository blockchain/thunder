package network.thunder.core.communication.layer.high.payments.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LNPaymentBMessage extends LNPayment implements LNRevokeNewMessage, LNRevokeOldMessage, LNSignatureMessage {

    public RevocationHash newRevocation;
    public List<RevocationHash> oldRevocation;

    public List<byte[]> channelSignatures = new ArrayList<>();
    public List<byte[]> paymentSignatures = new ArrayList<>();

    @Override
    public ChannelSignatures getChannelSignatures () {
        ChannelSignatures signatures = new ChannelSignatures();
        signatures.paymentSignatures = paymentSignatures.stream().map(o -> TransactionSignature.decodeFromBitcoin(o, true)).collect(Collectors.toList());
        signatures.channelSignatures = channelSignatures.stream().map(o -> TransactionSignature.decodeFromBitcoin(o, true)).collect(Collectors.toList());
        return signatures;
    }

    @Override
    public void setChannelSignatures (ChannelSignatures channelSignatures) {
        this.channelSignatures = channelSignatures.channelSignatures.stream().map(TransactionSignature::encodeToBitcoin).collect(Collectors.toList());
        this.paymentSignatures = channelSignatures.paymentSignatures.stream().map(TransactionSignature::encodeToBitcoin).collect(Collectors.toList());
    }

    @Override
    public RevocationHash getNewRevocationHash () {
        return newRevocation;
    }

    @Override
    public List<RevocationHash> getOldRevocationHash () {
        return oldRevocation;
    }

    @Override
    public void setOldRevocationHash (List<RevocationHash> oldRevocationHash) {
        this.oldRevocation = oldRevocationHash;
    }

    @Override
    public void setNewRevocationHash (RevocationHash newRevocationHash) {
        this.newRevocation = newRevocationHash;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(oldRevocation);
        Preconditions.checkNotNull(newRevocation);
        Preconditions.checkNotNull(channelSignatures);
        Preconditions.checkNotNull(paymentSignatures);
    }

    @Override
    public String toString () {
        return "LNPaymentBMessage{"+newRevocation+"}";
    }
}
