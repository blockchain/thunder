package network.thunder.core.communication.layer.low.authentication;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.low.authentication.messages.Authentication;
import network.thunder.core.communication.layer.low.authentication.messages.AuthenticationMessage;
import network.thunder.core.communication.layer.low.authentication.messages.AuthenticationMessageFactory;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.helper.crypto.CryptoTools;
import network.thunder.core.helper.events.LNEventHelper;
import org.bitcoinj.core.ECKey;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

public class AuthenticationProcessorImpl extends AuthenticationProcessor {

    AuthenticationMessageFactory messageFactory;
    LNEventHelper eventHelper;
    ClientObject node;
    ServerObject serverObject;

    MessageExecutor messageExecutor;

    public boolean sentAuth;
    public boolean authFinished;

    public AuthenticationProcessorImpl (ContextFactory contextFactory, ClientObject node) {
        this.messageFactory = contextFactory.getAuthenticationMessageFactory();
        this.eventHelper = contextFactory.getEventHelper();
        this.node = node;
        this.serverObject = contextFactory.getServerSettings();
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
            ECKey keyServer = serverObject.pubKeyServer;
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

    public void checkAuthenticationMessage (AuthenticationMessage authentication, ClientObject node) throws NoSuchProviderException,
            NoSuchAlgorithmException {

        ECKey ecKey = ECKey.fromPublicOnly(authentication.pubKeyServer);
        if (node.pubKeyClient != null) {
            //Must be an outgoing connection, check if the nodeKey is what we expect it to be
            if (!Arrays.equals(ecKey.getPubKey(), node.pubKeyClient.getPubKey())) {
                //We connected to the wrong node?
                System.out.println("Connected to wrong node? Expected: " + node.pubKeyClient.getPublicKeyAsHex() + ". Is: " + ecKey.getPublicKeyAsHex());
                authenticationFailed();
            }
        }

        node.pubKeyClient = ecKey;

        ECKey pubKeyClient = node.pubKeyClient;
        ECKey pubKeyTempServer = node.ephemeralKeyServer;

        byte[] data = new byte[pubKeyClient.getPubKey().length + pubKeyTempServer.getPubKey().length];
        System.arraycopy(pubKeyClient.getPubKey(), 0, data, 0, pubKeyClient.getPubKey().length);
        System.arraycopy(pubKeyTempServer.getPubKey(), 0, data, pubKeyClient.getPubKey().length, pubKeyTempServer.getPubKey().length);

        if (!CryptoTools.verifySignature(pubKeyClient, data, authentication.signature)) {
            System.out.println("Node was not able to authenticate..");
            authenticationFailed();
        }
    }

    private void authenticationFailed () {
        node.onAuthenticationFailed.stream().forEach(Command::execute);
        this.messageExecutor.closeConnection();
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
