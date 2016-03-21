package network.thunder.core.communication.layer.low.authentication.messages;

import network.thunder.core.communication.layer.MessageFactory;

/**
 * Created by matsjerratsch on 28/11/2015.
 */
public interface AuthenticationMessageFactory extends MessageFactory {

    AuthenticationMessage getAuthenticationMessage (byte[] pubkeyServer, byte[] signature);
}
