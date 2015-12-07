package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptedMessage;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptionInitialMessage;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public interface EncryptionMessageFactory extends MessageFactory {
    EncryptedMessage getEncryptedMessage (byte[] enc, byte[] hmac);

    EncryptionInitialMessage getEncryptionInitialMessage (byte[] key);
}
