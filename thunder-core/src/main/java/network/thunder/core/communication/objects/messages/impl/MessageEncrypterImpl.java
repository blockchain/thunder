package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptedMessage;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageEncrypter;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializer;
import network.thunder.core.etc.crypto.CryptoTools;
import network.thunder.core.etc.crypto.ECDHKeySet;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class MessageEncrypterImpl implements MessageEncrypter {

    public MessageEncrypterImpl (MessageSerializer serializater) {
        this.serializater = serializater;
    }

    MessageSerializer serializater;

    @Override
    public EncryptedMessage encrypt (Message data, ECDHKeySet keySet) {
        byte[] bytes = serializater.serializeMessage(data);

        byte[] enc = CryptoTools.encryptAES_CTR(bytes, keySet.encryptionKey, keySet.ivServer, keySet.counterOut);
        byte[] hmac = CryptoTools.getHMAC(enc, keySet.hmacKey);

        return new EncryptedMessage(hmac, enc);
    }

    @Override
    public Message decrypt (EncryptedMessage message, ECDHKeySet ecdhKeySet) {
        byte[] bytes = message.payload;

        CryptoTools.checkHMAC(message.hmac, message.payload, ecdhKeySet.hmacKey);

        bytes = CryptoTools.decryptAES_CTR(message.payload, ecdhKeySet.encryptionKey, ecdhKeySet.ivClient, ecdhKeySet.counterIn);

        Message decryptedMessage = serializater.deserializeMessage(bytes);

        return decryptedMessage;
    }
}
