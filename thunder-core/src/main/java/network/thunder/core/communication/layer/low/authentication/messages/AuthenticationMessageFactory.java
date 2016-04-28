package network.thunder.core.communication.layer.low.authentication.messages;

import network.thunder.core.communication.layer.MessageFactory;

public interface AuthenticationMessageFactory extends MessageFactory {

    AuthenticationMessage getAuthenticationMessage (byte[] pubkeyServer, byte[] signature);
}
