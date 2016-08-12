package network.thunder.core.communication.layer.middle.broadcasting.types;

import network.thunder.core.etc.Tools;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ChannelStatusObject extends P2PDataObject {

    public byte[] pubkeyA;
    public byte[] pubkeyB;
    public byte[] infoA;
    public byte[] infoB;

    public int latency;
    public short minTimeout;

    public Fee feeA;
    public Fee feeB;

    public byte[] signatureA;
    public byte[] signatureB;

    public int timestamp;

    public ChannelStatusObject () {
        feeA = Fee.ZERO_FEE;
        feeB = Fee.ZERO_FEE;
    }

    @Override
    public byte[] getData () {
        //TODO: Have some proper summary here..
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + pubkeyA.length + pubkeyB.length + 4 + 4 + 4 + 4 + 4 + 4);

        byteBuffer.putInt(timestamp);
        byteBuffer.put(pubkeyA);
        byteBuffer.put(pubkeyB);
        byteBuffer.putInt(latency);
        if (feeA != null) {
            byteBuffer.putInt(feeA.fix);
            byteBuffer.putInt(feeA.perc);
        }
        if (feeB != null) {
            byteBuffer.putInt(feeB.fix);
            byteBuffer.putInt(feeB.perc);
        }
        byteBuffer.putInt(minTimeout);

        return byteBuffer.array();
    }

    @Override
    public int getTimestamp () {
        return timestamp;
    }

    public Fee getFee (byte[] array) {
        if (Arrays.equals(array, pubkeyA)) {
            return feeA;
        } else {
            return feeB;
        }
    }

    public byte[] getOtherNode (byte[] node) {
        if (Arrays.equals(node, pubkeyA)) {
            return pubkeyB;
        } else {
            return pubkeyA;
        }
    }

    @Override
    public long getHashAsLong () {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(Tools.hashSecret(this.getData()), 0, 8);
        byteBuffer.flip();
        return Math.abs(byteBuffer.getLong());
    }

    public double getWeight (byte[] array, float weightPrivacy, float weightLatency, float weightCost) {
        return 1;
    }

    @Override
    public void verify () {
    }

    @Override
    public boolean isSimilarObject (P2PDataObject object) {
        if (object instanceof ChannelStatusObject) {
            ChannelStatusObject channel = (ChannelStatusObject) object;
            return Arrays.equals(channel.pubkeyA, this.pubkeyA) &&
                    Arrays.equals(channel.pubkeyB, this.pubkeyB);

        }
        return false;
    }

    @Override
    public String toString () {
        return "ChannelStatusObject{nodeA: " + Tools.bytesToHex(pubkeyA) + ", nodeB: " + Tools.bytesToHex(pubkeyB) + "}";
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChannelStatusObject that = (ChannelStatusObject) o;

        if (latency != that.latency) {
            return false;
        }
        if (feeA != that.feeA) {
            return false;
        }
        if (feeB != that.feeB) {
            return false;
        }
        if (timestamp != that.timestamp) {
            return false;
        }
        if (!Arrays.equals(pubkeyA, that.pubkeyA)) {
            return false;
        }
        if (!Arrays.equals(pubkeyB, that.pubkeyB)) {
            return false;
        }
        if (!Arrays.equals(infoA, that.infoA)) {
            return false;
        }
        return Arrays.equals(infoB, that.infoB);

    }

    @Override
    public int hashCode () {
        int result = Arrays.hashCode(pubkeyA);
        result = 31 * result + Arrays.hashCode(pubkeyB);
        result = 31 * result + Arrays.hashCode(infoA);
        result = 31 * result + Arrays.hashCode(infoB);
        result = 31 * result + latency;
        result = 31 * result + feeA.fix;
        result = 31 * result + feeB.fix;
        result = 31 * result + timestamp;
        return result;
    }
}
