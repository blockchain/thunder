package network.thunder.core.communication.layer.low.encryption.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.etc.Tools;

public class EncryptionInitialMessage extends Encryption {
    public byte[] key;

    public EncryptionInitialMessage (byte[] key) {
        this.key = key;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(key);
    }

    @Override
    public String toString () {
        return "EncryptionInitialMessage{" +
                "key=" + Tools.bytesToHex(key) +
                '}';
    }
}
