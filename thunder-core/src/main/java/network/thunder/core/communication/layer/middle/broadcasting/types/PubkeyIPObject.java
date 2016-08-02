package network.thunder.core.communication.layer.middle.broadcasting.types;

import network.thunder.core.etc.Tools;
import network.thunder.core.helper.crypto.CryptoTools;
import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;

public class PubkeyIPObject extends P2PDataObject {
    private static final Logger log = Tools.getLogger();
    public String hostname;
    public int port;
    public byte[] pubkey;
    public byte[] signature;
    public int timestamp;

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PubkeyIPObject ipObject = (PubkeyIPObject) o;

        if (port != ipObject.port) {
            return false;
        }
        if (timestamp != ipObject.timestamp) {
            return false;
        }
        if (hostname != null ? !hostname.equals(ipObject.hostname) : ipObject.hostname != null) {
            return false;
        }

        return Arrays.equals(pubkey, ipObject.pubkey);
    }

    @Override
    public byte[] getData () {
        //TODO: Have some proper summary here..
        ByteBuffer byteBuffer = ByteBuffer.allocate(hostname.length() + 4 + 4 + pubkey.length);
        try {
            byteBuffer.put(hostname.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.warn("", e);
        }
        byteBuffer.putInt(port);
        byteBuffer.put(pubkey);
        byteBuffer.putInt(timestamp);
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
    public int hashCode () {
        int result = hostname != null ? hostname.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + (pubkey != null ? Arrays.hashCode(pubkey) : 0);
        result = 31 * result + timestamp;
        return result;
    }

    public void sign (ECKey key) {
        try {
            this.signature = CryptoTools.createSignature(key, this.getData());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void verify () {
        //TODO: Implement signature verification..
    }

    @Override
    public boolean isSimilarObject (P2PDataObject object) {
        if (object instanceof PubkeyIPObject) {
            PubkeyIPObject channel = (PubkeyIPObject) object;
            boolean equals = channel.hostname.equals(this.hostname) && channel.port == this.port;
            return equals;
        }
        return false;
    }

    public void verifySignature () throws UnsupportedEncodingException, NoSuchProviderException, NoSuchAlgorithmException {
        if (!CryptoTools.verifySignature(ECKey.fromPublicOnly(pubkey), this.getData(), this.signature)) {
            throw new RuntimeException("Signature failed..");
        }
    }

    public static List<PubkeyIPObject> removeFromListByPubkey (List<PubkeyIPObject> fullList, List<PubkeyIPObject> toRemove) {
        List<PubkeyIPObject> temp = new ArrayList<>();
        for (PubkeyIPObject full : fullList) {
            for (PubkeyIPObject remove : toRemove) {
                if (Arrays.equals(full.pubkey, remove.pubkey)) {
                    temp.add(full);
                }
            }
        }
        fullList.removeAll(temp);
        return fullList;
    }

    public static List<PubkeyIPObject> removeFromListByPubkey (List<PubkeyIPObject> fullList, byte[] pubkey) {
        Iterator<PubkeyIPObject> iterator = fullList.iterator();
        while (iterator.hasNext()) {
            PubkeyIPObject object = iterator.next();
            if (Arrays.equals(object.pubkey, pubkey)) {
                iterator.remove();
            }
        }
        return fullList;
    }

    @Override
    public String toString () {
        return "PubkeyIPObject{" +
                "hostname='" + hostname + '\'' +
                ", port=" + port +
                ", timestamp=" + timestamp +
                '}';
    }
}
