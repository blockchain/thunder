package network.thunder.core.communication.layer.high.channel.close.messages;

import network.thunder.core.communication.layer.MesssageFactoryImpl;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

public class LNCloseMessageFactoryImpl extends MesssageFactoryImpl implements LNCloseMessageFactory {

    @Override
    public LNCloseAMessage getLNCloseAMessage (List<TransactionSignature> signatureList, float feePerByte) {
        return new LNCloseAMessage(signatureList, feePerByte);
    }
}
