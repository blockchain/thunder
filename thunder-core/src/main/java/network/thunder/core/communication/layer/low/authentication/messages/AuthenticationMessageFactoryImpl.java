package network.thunder.core.communication.layer.low.authentication.messages;

import network.thunder.core.communication.layer.MesssageFactoryImpl;

public class AuthenticationMessageFactoryImpl extends MesssageFactoryImpl implements AuthenticationMessageFactory {

    @Override
    public AuthenticationMessage getAuthenticationMessage (byte[] pubkeyServer, byte[] signature) {
        return new AuthenticationMessage(pubkeyServer, signature);
    }
}
