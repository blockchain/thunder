package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptedMessage;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptionInitialMessage;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public interface EncryptionMessageFactory extends MessageFactory {
    EncryptedMessage getEncryptedMessage (Message message, Node node);

    Message getDecryptedMessage (EncryptedMessage message, Node node);

    EncryptionInitialMessage getEncryptionInitialMessage (Node node);
}
