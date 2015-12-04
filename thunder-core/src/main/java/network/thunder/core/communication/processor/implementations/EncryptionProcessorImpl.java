package network.thunder.core.communication.processor.implementations;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.interfaces.factories.EncryptionMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.types.EncryptedMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.encryption.types.EncryptionInitial;
import network.thunder.core.communication.processor.interfaces.EncryptionProcessor;
import network.thunder.core.etc.crypto.ECDH;
import network.thunder.core.mesh.Node;
import org.bitcoinj.core.ECKey;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public class EncryptionProcessorImpl implements EncryptionProcessor {
    EncryptionMessageFactory messageFactory;
    Node node;

    MessageExecutor executor;

    boolean encSent;
    boolean encFinished;

    public EncryptionProcessorImpl (EncryptionMessageFactory messageFactory, Node node) {
        this.messageFactory = messageFactory;
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
    public void onInboundMessageMessage (Message message) {
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
            executor.sendMessageUpwards(messageFactory.getEncryptionInitialMessage(node));
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
        Message decryptedMessage = messageFactory.getDecryptedMessage(message, node);
        executor.sendMessageDownwards(decryptedMessage);
    }

    private void processEncryptionInitialMessage (Message message) {
        if (!(message instanceof EncryptionInitial)) {
            executor.sendMessageUpwards(messageFactory.getFailureMessage("Expecting EncryptionInitial Message.. " + message));
        } else {
            EncryptionInitial encryptionInitial = (EncryptionInitial) message;

            node.ephemeralKeyClient = ECKey.fromPublicOnly(encryptionInitial.getKey());
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
        Message encryptedMessage = messageFactory.getEncryptedMessage(message, node);
        executor.sendMessageUpwards(encryptedMessage);
    }

}
