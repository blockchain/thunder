package network.thunder.core.communication.layer.low.encryption;

import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptedMessage;
import network.thunder.core.communication.layer.low.serialisation.MessageSerializer;
import network.thunder.core.helper.crypto.CryptoTools;
import network.thunder.core.helper.crypto.ECDHKeySet;

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
        CryptoTools.checkHMAC(message.hmac, message.payload, ecdhKeySet.hmacKey);
        byte[] bytes = CryptoTools.decryptAES_CTR(message.payload, ecdhKeySet.encryptionKey, ecdhKeySet.ivClient, ecdhKeySet.counterIn);

        return serializater.deserializeMessage(bytes);
    }
}
