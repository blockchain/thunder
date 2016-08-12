package network.thunder.core.communication.layer.low.authentication.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.etc.Tools;

public class AuthenticationMessage extends Authentication {

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

    @Override
    public String toString () {
        return "AuthenticationMessage{" +
                "pubKeyServer=" + Tools.bytesToHex(pubKeyServer) +
                ", signature=" + Tools.bytesToHex(signature) +
                '}';
    }
}
