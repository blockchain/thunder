package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.interfaces.message.authentication.types.AuthenticationMessage;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 28/11/2015.
 */
public interface AuthenticationMessageFactory extends MessageFactory {

    public AuthenticationMessage getAuthenticationMessage (Node node);
}
