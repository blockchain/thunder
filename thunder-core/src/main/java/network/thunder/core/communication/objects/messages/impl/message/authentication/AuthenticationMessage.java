package network.thunder.core.communication.objects.messages.impl.message.authentication;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.authentication.Authentication;

/**
 * Created by matsjerratsch on 27/11/2015.
 */
public class AuthenticationMessage implements Authentication {

    public byte[] pubKeyServer;
    public byte[] signature;

    public AuthenticationMessage (byte[] pubKeyServer, byte[] signature) {
        this.pubKeyServer = pubKeyServer;
        this.signature = signature;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(pubKeyServer);
        Preconditions.checkNotNull(signature);
    }
}
