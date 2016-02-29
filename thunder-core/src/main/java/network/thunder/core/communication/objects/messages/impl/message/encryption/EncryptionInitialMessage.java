package network.thunder.core.communication.objects.messages.impl.message.encryption;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.Encryption;
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
