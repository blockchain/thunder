package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.authentication.AuthenticationMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.AuthenticationMessageFactory;

public class AuthenticationMessageFactoryImpl extends MesssageFactoryImpl implements AuthenticationMessageFactory {

    @Override
    public AuthenticationMessage getAuthenticationMessage (byte[] pubkeyServer, byte[] signature) {
        return new AuthenticationMessage(pubkeyServer, signature);
    }
}
