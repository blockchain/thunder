package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.etc.Tools;

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
}
