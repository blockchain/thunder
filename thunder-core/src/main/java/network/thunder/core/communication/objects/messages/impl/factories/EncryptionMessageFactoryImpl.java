package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptedMessage;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptionInitialMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.EncryptionMessageFactory;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public class EncryptionMessageFactoryImpl extends MesssageFactoryImpl implements EncryptionMessageFactory {

    @Override
    public EncryptedMessage getEncryptedMessage (byte[] enc, byte[] hmac) {
        return new EncryptedMessage(hmac, enc);
    }

    @Override
    public EncryptionInitialMessage getEncryptionInitialMessage (byte[] key) {
        return new EncryptionInitialMessage(key);
    }
}
