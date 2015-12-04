package network.thunder.core.communication.objects.messages.impl.message.encryption;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.types.EncryptionInitial;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public class EncryptionInitialImpl implements EncryptionInitial {
    byte[] key;

    public EncryptionInitialImpl (byte[] key) {
        this.key = key;
    }

    @Override
    public byte[] getKey () {
        return key;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(key);
    }
}
