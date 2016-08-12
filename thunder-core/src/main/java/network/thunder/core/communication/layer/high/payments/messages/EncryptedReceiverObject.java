package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.etc.Tools;

import java.util.Arrays;

public class EncryptedReceiverObject {
    public byte[] ephemeralPubKeySender;
    public byte[] ephemeralPubKeyHashReceiver;
    public byte[] hmac;
    public byte[] data;

    @Override
    public String toString () {
        return "EncryptedReceiverObject{" +
                "data=" + Tools.bytesToHex(data).substring(0, 10) + ".." +
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

        EncryptedReceiverObject that = (EncryptedReceiverObject) o;

        if (!Arrays.equals(ephemeralPubKeySender, that.ephemeralPubKeySender)) {
            return false;
        }
        if (!Arrays.equals(ephemeralPubKeyHashReceiver, that.ephemeralPubKeyHashReceiver)) {
            return false;
        }
        if (!Arrays.equals(hmac, that.hmac)) {
            return false;
        }
        return Arrays.equals(data, that.data);

    }

    @Override
    public int hashCode () {
        int result = Arrays.hashCode(ephemeralPubKeySender);
        result = 31 * result + Arrays.hashCode(ephemeralPubKeyHashReceiver);
        result = 31 * result + Arrays.hashCode(hmac);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
