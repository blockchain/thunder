package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.types.EncryptedMessage;
import network.thunder.core.etc.crypto.ECDHKeySet;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface MessageEncrypter {

    EncryptedMessage encrypt (Message data, ECDHKeySet keySet);

    Message decrypt (EncryptedMessage message, ECDHKeySet ecdhKeySet);
}
