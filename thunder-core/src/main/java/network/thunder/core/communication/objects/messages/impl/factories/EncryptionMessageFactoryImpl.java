package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptionInitialImpl;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageEncrypter;
import network.thunder.core.communication.objects.messages.interfaces.factories.EncryptionMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.types.EncryptedMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.types.EncryptionInitial;
import network.thunder.core.etc.crypto.ECDHKeySet;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public class EncryptionMessageFactoryImpl extends MesssageFactoryImpl implements EncryptionMessageFactory {

    MessageEncrypter encrypter;

    public EncryptionMessageFactoryImpl (MessageEncrypter encrypter) {
        this.encrypter = encrypter;
    }

    @Override
    public EncryptedMessage getEncryptedMessage (Message message, Node node) {

        byte[] enc;
        byte[] hmac;

        ECDHKeySet ecdhKeySet = node.ecdhKeySet;
        EncryptedMessage encryptedMessage = encrypter.encrypt(message, ecdhKeySet);
        return encryptedMessage;
    }

    @Override
    public Message getDecryptedMessage (EncryptedMessage message, Node node) {
        ECDHKeySet ecdhKeySet = node.ecdhKeySet;
        return encrypter.decrypt(message, ecdhKeySet);
    }

    @Override
    public EncryptionInitial getEncryptionInitialMessage (Node node) {
        return new EncryptionInitialImpl(node.ephemeralKeyServer.getPubKey());
    }
}
