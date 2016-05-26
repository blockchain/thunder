package network.thunder.core.communication.layer.low.encryption;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.high.AckMessage;
import network.thunder.core.communication.layer.high.NumberedMessage;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptedMessage;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptionInitialMessage;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptionMessageFactory;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.messages.Gossip;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.crypto.ECDH;
import org.bitcoinj.core.ECKey;

public class EncryptionProcessorImpl extends EncryptionProcessor {
    public static final boolean OUTPUT_MESSAGE = true;
    public static final boolean OUTPUT_GOSSIP = false;
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
        try {
            if (encryptionKeyExchangeFinished()) {
                processEncryptedMessage(message);
            } else {
                processEncryptionInitialMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("EncryptionProcessorImpl.onInboundMessage closing connection..");
            executor.closeConnection();
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
            if (decryptedMessage instanceof Gossip) {
                if (OUTPUT_GOSSIP) {
                    System.out.println("I: " + getClientName() + " " + decryptedMessage);
                }
            } else {
                System.out.println("I: " +
                        getMessageNumber(decryptedMessage) + " " +
                        getAckedMessageNumber(decryptedMessage) + " " +
                        getClientName() + " " +
                        decryptedMessage + "[" + (message.payload.length / 1024) + "]");
            }
        }
        executor.sendMessageDownwards(decryptedMessage);
    }

    private void processEncryptionInitialMessage (Message message) {
        if (!(message instanceof EncryptionInitialMessage)) {
            throw new RuntimeException("Expecting EncryptionInitial Message.. " + message);
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
        EncryptedMessage encryptedMessage = messageEncrypter.encrypt(message, node.ecdhKeySet);
        if (OUTPUT_MESSAGE) {
            if (message instanceof Gossip) {
                if (OUTPUT_GOSSIP) {
                    System.out.println("O: " + getClientName() + " " + message + "[" + (encryptedMessage.payload.length / 1024) + "]");
                }
            } else {
                System.out.println("O: " +
                        System.currentTimeMillis() + " " +
                        getMessageNumber(message) + " " +
                        getAckedMessageNumber(message) + " " +
                        getClientName() + " " +
                        message + "[" + (encryptedMessage.payload.length / 1024) + "]");
            }
        }
        executor.sendMessageUpwards(encryptedMessage);
    }

    private void logIncomingMessage(Message decryptedMessage, EncryptedMessage message) {
        System.out.println("I: " +
                getMessageNumber(decryptedMessage) + " " +
                getAckedMessageNumber(decryptedMessage) + " " +
                getClientName() + " " +
                decryptedMessage + "[" + (message.payload.length / 1024) + "]");
    }

    private String getClientName () {
        if (node.host == null) {
            if (node.nodeKey != null) {
                return Tools.bytesToHex(node.nodeKey.getPubKey()).substring(0, 8);
            } else {
                return null;
            }
        } else {
            return node.host;
        }
    }

    private static long getMessageNumber (Message m) {
        if (m instanceof NumberedMessage) {
            return ((NumberedMessage) m).getMessageNumber();
        } else {
            return -1;
        }
    }

    private static long getAckedMessageNumber (Message m) {
        if (m instanceof AckMessage) {
            return ((AckMessage) m).getMessageNumberToAck();
        } else {
            return -1;
        }
    }

}
