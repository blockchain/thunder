package network.thunder.core.communication.objects.messages.impl.message.gossip.objects;

import network.thunder.core.etc.Tools;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class PubkeyChannelObject extends P2PDataObject {

    public byte[] secretAHash;
    public byte[] secretBHash;
    public byte[] pubkeyB;
    public byte[] pubkeyB1;
    public byte[] pubkeyB2;
    public byte[] pubkeyA;
    public byte[] pubkeyA1;
    public byte[] pubkeyA2;
    public byte[] txidAnchor;
    public byte[] signatureA;
    public byte[] signatureB;

    public PubkeyChannelObject () {
    }

    public PubkeyChannelObject (ResultSet set) throws SQLException {
        this.secretAHash = set.getBytes("secret_a_hash");
        this.secretBHash = set.getBytes("secret_b_hash");
        this.pubkeyB1 = set.getBytes("pubkey_b1");
        this.pubkeyB2 = set.getBytes("pubkey_b2");
        this.pubkeyA1 = set.getBytes("pubkey_a1");
        this.pubkeyA2 = set.getBytes("pubkey_a2");
        this.txidAnchor = set.getBytes("txid_anchor");
        this.signatureA = set.getBytes("signature_a");
        this.signatureB = set.getBytes("signature_b");

        this.pubkeyA = set.getBytes("nodes_a_table.pubkey");
        this.pubkeyB = set.getBytes("nodes_b_table.pubkey");
    }

    public static PubkeyChannelObject getRandomObject () {
        PubkeyChannelObject obj = new PubkeyChannelObject();

        obj.secretAHash = Tools.getRandomByte(20);
        obj.secretBHash = Tools.getRandomByte(20);

        obj.pubkeyB = Tools.getRandomByte(33);
        obj.pubkeyB1 = Tools.getRandomByte(33);
        obj.pubkeyB2 = Tools.getRandomByte(33);

        obj.pubkeyA = Tools.getRandomByte(33);
        obj.pubkeyA1 = Tools.getRandomByte(33);
        obj.pubkeyA2 = Tools.getRandomByte(33);

        obj.txidAnchor = Tools.getRandomByte(32);

        obj.signatureA = Tools.getRandomByte(65);
        obj.signatureB = Tools.getRandomByte(65);

        return obj;
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

        if (!Arrays.equals(secretAHash, that.secretAHash)) {
            return false;
        }
        if (!Arrays.equals(secretBHash, that.secretBHash)) {
            return false;
        }
        if (!Arrays.equals(pubkeyB, that.pubkeyB)) {
            return false;
        }
        if (!Arrays.equals(pubkeyB1, that.pubkeyB1)) {
            return false;
        }
        if (!Arrays.equals(pubkeyB2, that.pubkeyB2)) {
            return false;
        }
        if (!Arrays.equals(pubkeyA, that.pubkeyA)) {
            return false;
        }
        if (!Arrays.equals(pubkeyA1, that.pubkeyA1)) {
            return false;
        }
        if (!Arrays.equals(pubkeyA2, that.pubkeyA2)) {
            return false;
        }
        if (!Arrays.equals(txidAnchor, that.txidAnchor)) {
            return false;
        }
        if (!Arrays.equals(signatureA, that.signatureA)) {
            return false;
        }
        return Arrays.equals(signatureB, that.signatureB);

    }

    @Override
    public byte[] getData () {
        //TODO: Have some proper summary here..
        ByteBuffer byteBuffer = ByteBuffer.allocate(secretAHash.length + secretBHash.length + pubkeyB.length + pubkeyB1.length + pubkeyB2.length + pubkeyA.length + pubkeyA1.length + pubkeyA2.length + txidAnchor.length + signatureA.length + signatureB.length);

        byteBuffer.put(secretAHash);
        byteBuffer.put(secretBHash);
        byteBuffer.put(pubkeyB);
        byteBuffer.put(pubkeyB1);
        byteBuffer.put(pubkeyB2);
        byteBuffer.put(pubkeyA);
        byteBuffer.put(pubkeyA1);
        byteBuffer.put(pubkeyA2);
        byteBuffer.put(txidAnchor);
        byteBuffer.put(signatureA);
        byteBuffer.put(signatureB);

        return byteBuffer.array();
    }

    @Override
    public long getHashAsLong () {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(Tools.hashSecret(this.getData()), 0, 8);
        byteBuffer.flip();
        return Math.abs(byteBuffer.getLong());
    }

    @Override
    public int hashCode () {
        int result = secretAHash != null ? Arrays.hashCode(secretAHash) : 0;
        result = 31 * result + (secretBHash != null ? Arrays.hashCode(secretBHash) : 0);
        result = 31 * result + (pubkeyB != null ? Arrays.hashCode(pubkeyB) : 0);
        result = 31 * result + (pubkeyB1 != null ? Arrays.hashCode(pubkeyB1) : 0);
        result = 31 * result + (pubkeyB2 != null ? Arrays.hashCode(pubkeyB2) : 0);
        result = 31 * result + (pubkeyA != null ? Arrays.hashCode(pubkeyA) : 0);
        result = 31 * result + (pubkeyA1 != null ? Arrays.hashCode(pubkeyA1) : 0);
        result = 31 * result + (pubkeyA2 != null ? Arrays.hashCode(pubkeyA2) : 0);
        result = 31 * result + (txidAnchor != null ? Arrays.hashCode(txidAnchor) : 0);
        result = 31 * result + (signatureA != null ? Arrays.hashCode(signatureA) : 0);
        result = 31 * result + (signatureB != null ? Arrays.hashCode(signatureB) : 0);
        return result;
    }

    @Override
    public String toString () {
        return "PubkeyChannelObject{" +
            "secretAHash=" + Arrays.toString(secretAHash) +
            ", secretBHash=" + Arrays.toString(secretBHash) +
            ", pubkeyB=" + Arrays.toString(pubkeyB) +
            ", pubkeyB1=" + Arrays.toString(pubkeyB1) +
            ", pubkeyB2=" + Arrays.toString(pubkeyB2) +
            ", pubkeyA=" + Arrays.toString(pubkeyA) +
            ", pubkeyA1=" + Arrays.toString(pubkeyA1) +
            ", pubkeyA2=" + Arrays.toString(pubkeyA2) +
            ", txidAnchor=" + Arrays.toString(txidAnchor) +
            ", signatureA=" + Arrays.toString(signatureA) +
            ", signatureB=" + Arrays.toString(signatureB) +
            '}';
    }

    @Override
    public void verify () {
    }
}
