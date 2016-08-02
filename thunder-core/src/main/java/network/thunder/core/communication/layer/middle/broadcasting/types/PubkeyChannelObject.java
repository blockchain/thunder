package network.thunder.core.communication.layer.middle.broadcasting.types;

import network.thunder.core.etc.Tools;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PubkeyChannelObject extends P2PDataObject {

    public byte[] nodeKeyA;
    public byte[] channelKeyA;

    public byte[] nodeKeyB;
    public byte[] channelKeyB;

    public byte[] txidAnchor;
    public int timestamp;

    public byte[] signatureA;
    public byte[] signatureB;

    public PubkeyChannelObject () {
    }

    @Override
    public byte[] getData () {
        //TODO: Have some proper summary here..
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + nodeKeyB.length + channelKeyB.length + nodeKeyA
                .length + channelKeyA.length + txidAnchor.length);

        byteBuffer.putInt(timestamp);
        byteBuffer.put(nodeKeyB);
        byteBuffer.put(channelKeyB);
        byteBuffer.put(nodeKeyA);
        byteBuffer.put(channelKeyA);
        byteBuffer.put(txidAnchor);

        return byteBuffer.array();
    }

    @Override
    public int getTimestamp () {
        return timestamp;
    }

    @Override
    public long getHashAsLong () {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(Tools.hashSecret(this.getData()), 0, 8);
        byteBuffer.flip();
        return Math.abs(byteBuffer.getLong());
    }

    @Override
    public String toString () {
        return "PubkeyChannelObject{" + Tools.bytesToHex(getHash()) + "}";
    }

    @Override
    public void verify () {
    }

    @Override
    public boolean isSimilarObject (P2PDataObject object) {
        if (object instanceof PubkeyChannelObject) {
            PubkeyChannelObject channel = (PubkeyChannelObject) object;
            return (Arrays.equals(channel.nodeKeyA, this.nodeKeyA) && Arrays.equals(channel.nodeKeyB, this.nodeKeyB)) ||
                    (Arrays.equals(channel.nodeKeyA, this.nodeKeyB) && Arrays.equals(channel.nodeKeyB, this.nodeKeyA));

        }
        return false;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PubkeyChannelObject that = (PubkeyChannelObject) o;

        if (timestamp != that.timestamp) {
            return false;
        }
        if (!Arrays.equals(nodeKeyB, that.nodeKeyB)) {
            return false;
        }
        if (!Arrays.equals(channelKeyB, that.channelKeyB)) {
            return false;
        }
        if (!Arrays.equals(nodeKeyA, that.nodeKeyA)) {
            return false;
        }
        if (!Arrays.equals(channelKeyA, that.channelKeyA)) {
            return false;
        }
        return Arrays.equals(txidAnchor, that.txidAnchor);

    }

    @Override
    public int hashCode () {
        int result = Arrays.hashCode(nodeKeyB);
        result = 31 * result + Arrays.hashCode(channelKeyB);
        result = 31 * result + Arrays.hashCode(nodeKeyA);
        result = 31 * result + Arrays.hashCode(channelKeyA);
        result = 31 * result + Arrays.hashCode(txidAnchor);
        result = 31 * result + timestamp;
        return result;
    }
}
