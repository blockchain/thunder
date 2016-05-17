package network.thunder.core.communication.layer.middle.broadcasting.types;

import network.thunder.core.etc.Tools;

import javax.persistence.Entity;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
@Entity
public class PubkeyChannelObject extends P2PDataObject {

    public byte[] pubkeyB;
    public byte[] pubkeyB1;
    public byte[] pubkeyA;
    public byte[] pubkeyA1;
    public byte[] txidAnchor;
    public byte[] signatureA;
    public byte[] signatureB;

    public int timestamp;

    public PubkeyChannelObject () {
    }

    public PubkeyChannelObject (ResultSet set) throws SQLException {
        this.pubkeyB1 = set.getBytes("pubkey_b1");
        this.pubkeyA1 = set.getBytes("pubkey_a1");
        this.txidAnchor = set.getBytes("txid_anchor");
        this.signatureA = set.getBytes("signature_a");
        this.signatureB = set.getBytes("signature_b");

        this.pubkeyA = set.getBytes("nodes_a_table.pubkey");
        this.pubkeyB = set.getBytes("nodes_b_table.pubkey");
    }

    public static PubkeyChannelObject getRandomObject () {
        PubkeyChannelObject obj = new PubkeyChannelObject();


        obj.pubkeyB = Tools.getRandomByte(33);
        obj.pubkeyB1 = Tools.getRandomByte(33);

        obj.pubkeyA = Tools.getRandomByte(33);
        obj.pubkeyA1 = Tools.getRandomByte(33);

        obj.txidAnchor = Tools.getRandomByte(32);

        obj.signatureA = Tools.getRandomByte(65);
        obj.signatureB = Tools.getRandomByte(65);

        obj.timestamp = Tools.currentTime();

        return obj;
    }

    @Override
    public byte[] getData () {
        //TODO: Have some proper summary here..
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + pubkeyB.length + pubkeyB1.length + pubkeyA
                .length + pubkeyA1.length + txidAnchor.length);

        byteBuffer.putInt(timestamp);
        byteBuffer.put(pubkeyB);
        byteBuffer.put(pubkeyB1);
        byteBuffer.put(pubkeyA);
        byteBuffer.put(pubkeyA1);
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
            return (Arrays.equals(channel.pubkeyA, this.pubkeyA) && Arrays.equals(channel.pubkeyB, this.pubkeyB)) ||
                    (Arrays.equals(channel.pubkeyA, this.pubkeyB) && Arrays.equals(channel.pubkeyB, this.pubkeyA));

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
        if (!Arrays.equals(pubkeyB, that.pubkeyB)) {
            return false;
        }
        if (!Arrays.equals(pubkeyB1, that.pubkeyB1)) {
            return false;
        }
        if (!Arrays.equals(pubkeyA, that.pubkeyA)) {
            return false;
        }
        if (!Arrays.equals(pubkeyA1, that.pubkeyA1)) {
            return false;
        }
        return Arrays.equals(txidAnchor, that.txidAnchor);

    }

    @Override
    public int hashCode () {
        int result = Arrays.hashCode(pubkeyB);
        result = 31 * result + Arrays.hashCode(pubkeyB1);
        result = 31 * result + Arrays.hashCode(pubkeyA);
        result = 31 * result + Arrays.hashCode(pubkeyA1);
        result = 31 * result + Arrays.hashCode(txidAnchor);
        result = 31 * result + timestamp;
        return result;
    }
}
