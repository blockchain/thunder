package network.thunder.core.communication.layer.high.channel.close.messages;

import com.google.common.base.Preconditions;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LNCloseAMessage extends LNClose {
    public byte[] channelHash;
    public List<byte[]> signatureList;
    public float feePerByte;

    public LNCloseAMessage (Sha256Hash channelHash, Collection<TransactionSignature> signatureList, float feePerByte) {
        this.channelHash = channelHash.getBytes();
        this.signatureList = signatureList.stream().map(TransactionSignature::encodeToBitcoin).collect(Collectors.toList());
        this.feePerByte = feePerByte;
    }

    public List<TransactionSignature> getSignatureList () {
        return signatureList.stream().map(bytes -> TransactionSignature.decodeFromBitcoin(bytes, true)).collect(Collectors.toList());
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(signatureList);
        Preconditions.checkArgument(signatureList.size() > 0);
    }
}
