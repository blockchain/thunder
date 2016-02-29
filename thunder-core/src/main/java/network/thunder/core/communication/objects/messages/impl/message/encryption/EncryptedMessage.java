package network.thunder.core.communication.objects.messages.impl.message.encryption;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.Encryption;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public class EncryptedMessage implements Encryption {
    public byte[] hmac;
    public byte[] payload;

    public EncryptedMessage (byte[] hmac, byte[] payload) {
        this.hmac = hmac;
        this.payload = payload;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(hmac);
        Preconditions.checkNotNull(payload);
    }

    @Override
    public String toString () {
        return "EncryptedMessage{Size=" + payload.length / 1024 + "kB}";
    }
}
