package network.thunder.core.communication.layer.high.payments.messages;

import org.bitcoinj.core.Sha256Hash;

public class OnionObject {
    public final static int MAX_HOPS = 10;

    public final static int KEY_LENGTH = 33;
    public final static int HMAC_LENGTH = 20;
    public final static int DATA_LENGTH = 50;
    public final static int TOTAL_LENGTH = KEY_LENGTH + HMAC_LENGTH + DATA_LENGTH;

    public byte[] data;

    public OnionObject (byte[] data) {
        this.data = data;
    }

    @Override
    public String toString () {
        return "OnionObject{" +
                "data=" + Sha256Hash.of(data).toString() +
                '}';
    }
}
