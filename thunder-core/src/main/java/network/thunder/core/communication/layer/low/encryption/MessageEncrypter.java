package network.thunder.core.communication.layer.low.encryption;

import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptedMessage;
import network.thunder.core.helper.crypto.ECDHKeySet;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface MessageEncrypter {

    EncryptedMessage encrypt (Message data, ECDHKeySet keySet);

    Message decrypt (EncryptedMessage message, ECDHKeySet ecdhKeySet);
}
