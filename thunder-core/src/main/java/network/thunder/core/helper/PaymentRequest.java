package network.thunder.core.helper;

import network.thunder.core.communication.layer.high.payments.PaymentSecret;

import java.nio.ByteBuffer;

public class PaymentRequest {
    public long amount;
    public PaymentSecret paymentSecret;
    public byte[] pubkey;

    public byte[] getPayload () {

        ByteBuffer buffer = ByteBuffer.allocate(33 + 8 + 20);

        buffer.putLong(amount);
        buffer.put(paymentSecret.hash);
        buffer.put(pubkey);

        return buffer.array();
    }
}
