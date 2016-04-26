package network.thunder.core.communication.layer.high.channel.close.messages;

import network.thunder.core.communication.layer.MesssageFactoryImpl;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

public class LNCloseMessageFactoryImpl extends MesssageFactoryImpl implements LNCloseMessageFactory {

    @Override
    public LNCloseAMessage getLNCloseAMessage (Sha256Hash channelHash, List<TransactionSignature> signatureList, float feePerByte) {
        return new LNCloseAMessage(channelHash, signatureList, feePerByte);
    }
}
