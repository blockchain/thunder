package network.thunder.core.communication.objects.messages.interfaces.message.authentication.types;

import network.thunder.core.communication.objects.messages.interfaces.message.authentication.Authentication;

/**
 * Created by matsjerratsch on 27/11/2015.
 */
public interface AuthenticationMessage extends Authentication {

    public byte[] getPubkeyServer ();

    public byte[] getSignature ();
}
