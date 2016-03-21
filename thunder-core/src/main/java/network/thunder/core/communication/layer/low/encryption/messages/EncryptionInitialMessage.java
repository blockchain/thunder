package network.thunder.core.communication.layer.low.encryption.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.etc.Tools;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public class EncryptionInitialMessage implements Encryption {
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
