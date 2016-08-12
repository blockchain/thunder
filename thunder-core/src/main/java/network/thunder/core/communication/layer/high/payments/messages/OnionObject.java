package network.thunder.core.communication.layer.high.payments.messages;

import org.bitcoinj.core.Sha256Hash;

import java.util.Arrays;

public class OnionObject {
    public final static int MAX_HOPS = 10;

    public final static int KEY_LENGTH = 33;
    public final static int HMAC_LENGTH = 20;
    public final static int DATA_LENGTH = 60;
    public final static int TOTAL_LENGTH = KEY_LENGTH + HMAC_LENGTH + DATA_LENGTH;

    public byte[] data;
    public EncryptedReceiverObject dataFinalReceiver;

    public OnionObject (byte[] data) {
        this.data = data;
    }

    @Override
    public String toString () {
        return "OnionObject{" +
                "data=" + Sha256Hash.of(data).toString() +
                '}';
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OnionObject that = (OnionObject) o;

        if (!Arrays.equals(data, that.data)) {
            return false;
        }
        return dataFinalReceiver != null ? dataFinalReceiver.equals(that.dataFinalReceiver) : that.dataFinalReceiver == null;

    }

    @Override
    public int hashCode () {
        int result = Arrays.hashCode(data);
        result = 31 * result + (dataFinalReceiver != null ? dataFinalReceiver.hashCode() : 0);
        return result;
    }
}
