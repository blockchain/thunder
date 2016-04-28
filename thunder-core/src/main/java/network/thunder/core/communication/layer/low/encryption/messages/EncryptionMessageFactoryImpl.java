package network.thunder.core.communication.layer.low.encryption.messages;

import network.thunder.core.communication.layer.MesssageFactoryImpl;

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
