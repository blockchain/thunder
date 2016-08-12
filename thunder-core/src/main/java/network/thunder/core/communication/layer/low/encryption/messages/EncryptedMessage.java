package network.thunder.core.communication.layer.low.encryption.messages;

import com.google.common.base.Preconditions;

public class EncryptedMessage extends Encryption {
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
