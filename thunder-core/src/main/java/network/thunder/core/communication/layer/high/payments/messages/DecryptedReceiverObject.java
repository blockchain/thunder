package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.layer.high.payments.PaymentSecret;

import java.nio.ByteBuffer;

public class DecryptedReceiverObject {
    public long amount;
    public PaymentSecret secret;

    public DecryptedReceiverObject () {
    }

    public DecryptedReceiverObject (byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        amount = byteBuffer.getLong();
        byte[] secret = new byte[20];
        byteBuffer.get(secret);
        this.secret = new PaymentSecret(secret);
    }

    public byte[] getData () {
        ByteBuffer byteBuffer = ByteBuffer.allocate(28);
        byteBuffer.putLong(amount);
        byteBuffer.put(secret.secret);
        return byteBuffer.array();
    }
}
