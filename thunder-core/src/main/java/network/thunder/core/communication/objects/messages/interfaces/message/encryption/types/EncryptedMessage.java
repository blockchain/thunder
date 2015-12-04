package network.thunder.core.communication.objects.messages.interfaces.message.encryption.types;

import network.thunder.core.communication.objects.messages.interfaces.message.encryption.Encryption;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public interface EncryptedMessage extends Encryption {
    byte[] getHMAC ();

    byte[] getEncryptedBytes ();
}
