package network.thunder.core.communication.objects.messages.impl.message.authentication;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.authentication.types.AuthenticationMessage;

/**
 * Created by matsjerratsch on 27/11/2015.
 */
public class AuthenticationMessageImpl implements AuthenticationMessage {

    byte[] pubKeyServer;
    byte[] signature;

    public AuthenticationMessageImpl (byte[] pubKeyServer, byte[] signature) {
        this.pubKeyServer = pubKeyServer;
        this.signature = signature;
    }

    @Override
    public byte[] getPubkeyServer () {
        return pubKeyServer;
    }

    @Override
    public byte[] getSignature () {
        return signature;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(pubKeyServer);
        Preconditions.checkNotNull(signature);
    }
}
