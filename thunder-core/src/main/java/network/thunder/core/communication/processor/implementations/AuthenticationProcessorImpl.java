package network.thunder.core.communication.processor.implementations;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.message.authentication.AuthenticationMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.AuthenticationMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.message.authentication.Authentication;
import network.thunder.core.communication.processor.interfaces.AuthenticationProcessor;
import network.thunder.core.etc.crypto.CryptoTools;
import network.thunder.core.mesh.Node;
import org.bitcoinj.core.ECKey;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class AuthenticationProcessorImpl extends AuthenticationProcessor {

    AuthenticationMessageFactory messageFactory;
    LNEventHelper eventHelper;
    Node node;

    MessageExecutor messageExecutor;

    public boolean sentAuth;
    public boolean authFinished;

    public AuthenticationProcessorImpl (ContextFactory contextFactory, NodeClient node) {
        this.messageFactory = contextFactory.getAuthenticationMessageFactory();
        this.eventHelper = contextFactory.getEventHelper();
        this.node = node;
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;

        if (shouldSendAuthenticationFirst()) {
            sendAuthentication();
        }
    }

    @Override
    public void onInboundMessage (Message message) {
        if (message instanceof Authentication) {
            processMessage(message);
        } else if (!authenticationExchangeFinished()) {
            sendAuthenticatedErrorMessage("Not authenticated..");
        } else {
            passMessageToNextLayer(message);
        }
    }

    @Override
    public void onOutboundMessage (Message message) {
        if (allowOutboundMessage(message)) {
            messageExecutor.sendMessageUpwards(message);
        } else {
            throw new RuntimeException("Should not happen, which class is this? " + message);
        }

    }

    public boolean shouldSendAuthenticationFirst () {
        return !node.isServer;
    }

    public void sendAuthentication () {
        if (!sentAuth) {
            messageExecutor.sendMessageUpwards(getAuthenticationMessage());
            sentAuth = true;
        }
    }

    public void processMessage (Message message) {
        if (authenticationExchangeFinished()) {
            messageExecutor.sendMessageUpwards(messageFactory.getFailureMessage("Already authenticated"));

        } else {
            processAuthenticationMessage(message);
        }
    }

    public boolean allowOutboundMessage (Message message) {
        return message instanceof Authentication || authFinished;
    }

    public void processAuthenticationMessage (Message message) {
        AuthenticationMessage authObject = (AuthenticationMessage) message;

        try {

            checkAuthenticationMessage(authObject, node);
            sendAuthentication();
            authFinished = true;
            eventHelper.onConnectionOpened(node);
            messageExecutor.sendNextLayerActive();

        } catch (Exception e) {
            e.printStackTrace();
            messageExecutor.sendMessageUpwards(messageFactory.getFailureMessage(e.getMessage()));
        }

    }

    @Override
    public void onLayerClose () {
        eventHelper.onConnectionClosed(node);
    }

    public AuthenticationMessage getAuthenticationMessage () {
        try {
            ECKey keyServer = node.pubKeyServer;
            ECKey keyClient = node.ephemeralKeyClient;

            byte[] data = new byte[keyServer.getPubKey().length + keyClient.getPubKey().length];
            System.arraycopy(keyServer.getPubKey(), 0, data, 0, keyServer.getPubKey().length);
            System.arraycopy(keyClient.getPubKey(), 0, data, keyServer.getPubKey().length, keyClient.getPubKey().length);

            byte[] pubkeyServer = keyServer.getPubKey();
            byte[] signature = CryptoTools.createSignature(keyServer, data);

            return messageFactory.getAuthenticationMessage(pubkeyServer, signature);

        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkAuthenticationMessage (AuthenticationMessage authentication, Node node) throws NoSuchProviderException,
            NoSuchAlgorithmException {

        //TODO: Check whether the pubkeyClient is actually the pubkey we are expecting
        node.pubKeyClient = ECKey.fromPublicOnly(authentication.pubKeyServer);

        ECKey pubKeyClient = node.pubKeyClient;
        ECKey pubKeyTempServer = node.ephemeralKeyServer;

        byte[] data = new byte[pubKeyClient.getPubKey().length + pubKeyTempServer.getPubKey().length];
        System.arraycopy(pubKeyClient.getPubKey(), 0, data, 0, pubKeyClient.getPubKey().length);
        System.arraycopy(pubKeyTempServer.getPubKey(), 0, data, pubKeyClient.getPubKey().length, pubKeyTempServer.getPubKey().length);

        CryptoTools.verifySignature(pubKeyClient, data, authentication.signature);
    }

    private boolean authenticationExchangeFinished () {
        return authFinished;
    }

    private void sendAuthenticatedErrorMessage (String error) {
        messageExecutor.sendMessageUpwards(messageFactory.getFailureMessage(error));
    }

    private void passMessageToNextLayer (Message message) {
        messageExecutor.sendMessageDownwards(message);
    }

}
