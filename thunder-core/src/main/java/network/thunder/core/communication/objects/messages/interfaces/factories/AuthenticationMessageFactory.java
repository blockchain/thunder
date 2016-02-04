package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.impl.message.authentication.AuthenticationMessage;

/**
 * Created by matsjerratsch on 28/11/2015.
 */
public interface AuthenticationMessageFactory extends MessageFactory {

    AuthenticationMessage getAuthenticationMessage (byte[] pubkeyServer, byte[] signature);
}
