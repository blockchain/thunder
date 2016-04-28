package network.thunder.core.communication.layer.low.encryption.messages;

import network.thunder.core.communication.layer.MessageFactory;

public interface EncryptionMessageFactory extends MessageFactory {
    EncryptedMessage getEncryptedMessage (byte[] enc, byte[] hmac);

    EncryptionInitialMessage getEncryptionInitialMessage (byte[] key);
}
