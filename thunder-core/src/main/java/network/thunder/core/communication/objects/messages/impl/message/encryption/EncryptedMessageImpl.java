package network.thunder.core.communication.objects.messages.impl.message.encryption;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.types.EncryptedMessage;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public class EncryptedMessageImpl implements EncryptedMessage {
    byte[] hmac;
    byte[] payload;

    public EncryptedMessageImpl (byte[] hmac, byte[] payload) {
        this.hmac = hmac;
        this.payload = payload;
    }

    @Override
    public byte[] getHMAC () {
        return hmac;
    }

    @Override
    public byte[] getEncryptedBytes () {
        return payload;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(hmac);
        Preconditions.checkNotNull(payload);
    }
}
