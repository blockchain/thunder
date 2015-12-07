package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.authentication.AuthenticationMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.AuthenticationMessageFactory;
import network.thunder.core.etc.crypto.CryptoTools;
import network.thunder.core.mesh.Node;
import org.bitcoinj.core.ECKey;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class AuthenticationMessageFactoryImpl extends MesssageFactoryImpl implements AuthenticationMessageFactory {

    public AuthenticationMessage getAuthenticationMessage (Node node) {
        try {
            ECKey keyServer = node.pubKeyServer;
            ECKey keyClient = node.ephemeralKeyClient;

            byte[] data = new byte[keyServer.getPubKey().length + keyClient.getPubKey().length];
            System.arraycopy(keyServer.getPubKey(), 0, data, 0, keyServer.getPubKey().length);
            System.arraycopy(keyClient.getPubKey(), 0, data, keyServer.getPubKey().length, keyClient.getPubKey().length);

            byte[] pubkeyServer = keyServer.getPubKey();
            byte[] signature = CryptoTools.createSignature(keyServer, data);

            return new AuthenticationMessage(pubkeyServer, signature);

        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
