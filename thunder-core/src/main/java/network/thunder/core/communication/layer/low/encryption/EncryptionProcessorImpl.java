package network.thunder.core.communication.layer.low.encryption;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptedMessage;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptionInitialMessage;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptionMessageFactory;
import network.thunder.core.helper.crypto.ECDH;
import org.bitcoinj.core.ECKey;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public class EncryptionProcessorImpl extends EncryptionProcessor {
    public static final boolean OUTPUT_MESSAGE = true;
    EncryptionMessageFactory messageFactory;
    MessageEncrypter messageEncrypter;
    ClientObject node;

    MessageExecutor executor;

    boolean encSent;
    boolean encFinished;

    public EncryptionProcessorImpl (ContextFactory contextFactory, ClientObject node) {
        this.messageFactory = contextFactory.getEncryptionMessageFactory();
        this.messageEncrypter = contextFactory.getMessageEncrypter();
        this.node = node;
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.executor = messageExecutor;
        if (shouldSendEncryptionKeyFirst()) {
            sendInitialMessageIfNotSent();
        }
    }

    @Override
    public void onInboundMessage (Message message) {
        if (encryptionKeyExchangeFinished()) {
            processEncryptedMessage(message);
        } else {
            processEncryptionInitialMessage(message);
        }
    }

    @Override
    public void onOutboundMessage (Message message) {
        if (encryptionKeyExchangeFinished()) {
            processMessageToBeEncrypted(message);
        } else {
            throw new RuntimeException("Outbound Message even though Key Exchanged not finished yet.. " + message);
        }
    }

    private boolean shouldSendEncryptionKeyFirst () {
        return !node.isServer;
    }

    private void sendInitialMessageIfNotSent () {
        if (!encSent) {
            executor.sendMessageUpwards(messageFactory.getEncryptionInitialMessage(node.ephemeralKeyServer.getPubKey()));
            encSent = true;
        }
    }

    private boolean encryptionKeyExchangeFinished () {
        return encFinished;
    }

    private void processEncryptedMessage (Message message) {
        if (!(message instanceof EncryptedMessage)) {
            throw new RuntimeException("Non-encrypted message after key exchange..? " + message);
        }

        EncryptedMessage encryptedMessage = (EncryptedMessage) message;
        processMessageToBeDecrypted(encryptedMessage);
    }

    private void processMessageToBeDecrypted (EncryptedMessage message) {
        Message decryptedMessage = messageEncrypter.decrypt(message, node.ecdhKeySet);
        if (OUTPUT_MESSAGE) {
            System.out.println("I: " + node.host + " " + decryptedMessage);
        }
        executor.sendMessageDownwards(decryptedMessage);
    }

    private void processEncryptionInitialMessage (Message message) {
        if (!(message instanceof EncryptionInitialMessage)) {
            executor.sendMessageUpwards(messageFactory.getFailureMessage("Expecting EncryptionInitial Message.. " + message));
        } else {
            EncryptionInitialMessage encryptionInitial = (EncryptionInitialMessage) message;

            node.ephemeralKeyClient = ECKey.fromPublicOnly(encryptionInitial.key);
            node.ecdhKeySet = ECDH.getSharedSecret(node.ephemeralKeyServer, node.ephemeralKeyClient);

            sendInitialMessageIfNotSent();
            onKeyExchangeFinished();
        }
    }

    private void onKeyExchangeFinished () {
        encFinished = true;
        executor.sendNextLayerActive();
    }

    private void processMessageToBeEncrypted (Message message) {
        if (OUTPUT_MESSAGE) {
            System.out.println("O: " + node.host + " " + message);
        }

        Message encryptedMessage = messageEncrypter.encrypt(message, node.ecdhKeySet);
        executor.sendMessageUpwards(encryptedMessage);
    }

}
