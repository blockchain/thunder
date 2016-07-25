package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PeeledOnion {
    private static final Logger log = Tools.getLogger();

    static final int TEST_LAST_HOP_BYTE_AMOUNT = 10;
    static final byte[] emptyData = new byte[TEST_LAST_HOP_BYTE_AMOUNT];

    //If last hop, the first 10B will be zeros
    //We could have a simple flag somewhere, but the onion object is constant size anyways, so that would actually
    //increase the total size.
    public boolean isLastHop;

    //If this is the last hop, the first byte will indicate whether it contains a payment secret,
    //which is stored in the next 20B.
    public boolean containsSecret;
    public PaymentSecret paymentSecret;

    public NodeKey nextHop;

    public long amount = 0;
    public int fee = 0;

    public int timeout = 0;
    public short timeoutRemoved = 0;

    public OnionObject onionObject;

    public PeeledOnion () {
    }

    public PeeledOnion (OnionObject onionObject, byte[] data) {
        this.onionObject = onionObject;
        parseMessage(data);
    }

    void parseMessage (byte[] data) {
        byte[] pubkeyOfNextHop = new byte[33];
        byte[] firstTenBytes = new byte[10];
        System.arraycopy(data, 0, pubkeyOfNextHop, 0, 33);
        System.arraycopy(data, 0, firstTenBytes, 0, TEST_LAST_HOP_BYTE_AMOUNT);

        if (Arrays.equals(emptyData, firstTenBytes)) {
            isLastHop = true;
            if (data[TEST_LAST_HOP_BYTE_AMOUNT] == 1) {
                containsSecret = true;
                byte[] secret = new byte[20];
                System.arraycopy(data, TEST_LAST_HOP_BYTE_AMOUNT + 1, secret, 0, 20);
                paymentSecret = new PaymentSecret(secret);
            } else {
                containsSecret = false;
            }

        } else {
            log.debug("PeeledOnion.parseMessage "+ Tools.bytesToHex(pubkeyOfNextHop));
            nextHop = new NodeKey(ECKey.fromPublicOnly(pubkeyOfNextHop));
        }
        byte[] metaDataByteArray = new byte[data.length - OnionObject.KEY_LENGTH];
        System.arraycopy(data, OnionObject.KEY_LENGTH, metaDataByteArray, 0, metaDataByteArray.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(metaDataByteArray);

        amount = byteBuffer.getLong();
        fee = byteBuffer.getInt();
        timeout = byteBuffer.getInt();
        timeoutRemoved = byteBuffer.getShort();
    }

    public byte[] getData() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(OnionObject.DATA_LENGTH);
        if(isLastHop) {
            byteBuffer.put(emptyData);
            if(containsSecret) {
                byte flag = 1;
                byteBuffer.put(flag);
                byteBuffer.put(paymentSecret.secret);
                byteBuffer.put(new byte[2]);
            } else {
                byteBuffer.put(new byte[23]);
            }
        } else {
            byteBuffer.put(nextHop.getPubKey());
        }
        byteBuffer.putLong(amount);
        byteBuffer.putInt(fee);
        byteBuffer.putInt(timeout);
        byteBuffer.putShort(timeoutRemoved);
        log.debug("PeeledOnion.getData "+Tools.bytesToHex(byteBuffer.array()));
        return byteBuffer.array();
    }
}
