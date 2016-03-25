package network.thunder.core.communication.layer.high.channel.close.messages;

import network.thunder.core.communication.layer.MessageFactory;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

public interface LNCloseMessageFactory extends MessageFactory {
    LNCloseAMessage getLNCloseAMessage (List<TransactionSignature> signatureList, float feePerByte);
}
