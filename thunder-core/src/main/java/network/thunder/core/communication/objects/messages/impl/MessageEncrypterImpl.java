package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptedMessageImpl;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageEncrypter;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializater;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.types.EncryptedMessage;
import network.thunder.core.etc.crypto.CryptoTools;
import network.thunder.core.etc.crypto.ECDHKeySet;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class MessageEncrypterImpl implements MessageEncrypter {

    public MessageEncrypterImpl (MessageSerializater serializater) {
        this.serializater = serializater;
    }

    MessageSerializater serializater;

    @Override
    public EncryptedMessage encrypt (Message data, ECDHKeySet keySet) {
        byte[] bytes = serializater.serializeMessage(data);

        byte[] enc = CryptoTools.encryptAES_CTR(bytes, keySet.encryptionKey, keySet.ivServer, keySet.counterOut);
        byte[] hmac = CryptoTools.getHMAC(enc, keySet.hmacKey);

        return new EncryptedMessageImpl(hmac, enc);
    }

    @Override
    public Message decrypt (EncryptedMessage message, ECDHKeySet ecdhKeySet) {
        byte[] bytes = message.getEncryptedBytes();

        bytes = CryptoTools.checkAndRemoveHMAC(message, ecdhKeySet.hmacKey);
        bytes = CryptoTools.decryptAES_CTR(bytes, ecdhKeySet.encryptionKey, ecdhKeySet.ivClient, ecdhKeySet.counterIn);

        Message decryptedMessage = serializater.deserializeMessage(bytes);

        return decryptedMessage;
    }
}
