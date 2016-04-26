package network.thunder.core.communication.layer.high.channel.close.messages;

import network.thunder.core.communication.layer.MessageFactory;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

public interface LNCloseMessageFactory extends MessageFactory {
    LNCloseAMessage getLNCloseAMessage (Sha256Hash channelHash, List<TransactionSignature> signatureList, float feePerByte);
}
