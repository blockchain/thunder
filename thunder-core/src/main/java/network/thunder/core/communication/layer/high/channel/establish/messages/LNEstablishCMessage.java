package network.thunder.core.communication.layer.high.channel.establish.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.Arrays;

public class LNEstablishCMessage implements LNEstablish {
    public byte[] signatureEscape;
    public byte[] signatureFastEscape;
    public byte[] anchorHash;

    public LNEstablishCMessage (byte[] signatureEscape, byte[] signatureFastEscape, byte[] anchorHash) {
        this.signatureEscape = signatureEscape;
        this.signatureFastEscape = signatureFastEscape;
        this.anchorHash = anchorHash;
    }

    @Override
    public void saveToChannel (Channel channel) {
        channel.setAnchorTxHashClient(Sha256Hash.wrap(this.anchorHash));
        channel.setEscapeTxSig(TransactionSignature.decodeFromBitcoin(this.signatureEscape, true));
        channel.setFastEscapeTxSig(TransactionSignature.decodeFromBitcoin(this.signatureFastEscape, true));
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(signatureEscape);
        Preconditions.checkNotNull(signatureFastEscape);
        Preconditions.checkNotNull(anchorHash);
    }

    @Override
    public String toString () {
        return "LNEstablishCMessage{" +
                "anchorHash=" + Arrays.toString(anchorHash) +
                '}';
    }
}
