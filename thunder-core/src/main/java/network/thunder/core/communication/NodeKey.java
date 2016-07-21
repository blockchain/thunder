package network.thunder.core.communication;

import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;

import java.util.Arrays;

public class NodeKey {
    ECKey nodeKey;

    public NodeKey (byte[] nodeKey) {
        this.nodeKey = ECKey.fromPublicOnly(nodeKey);
    }

    public NodeKey (ECKey nodeKey) {
        this.nodeKey = nodeKey;
    }

    public byte[] getPubKey () {
        return nodeKey.getPubKey();
    }

    public ECKey getECKey () {
        return nodeKey;
    }

    public String getPubKeyHex () {
        return Tools.bytesToHex(nodeKey.getPubKey());
    }

    @Override
    public String toString () {
        return getPubKeyHex().substring(0, 10);
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NodeKey node = (NodeKey) o;

        return Arrays.equals(nodeKey.getPubKey(), node.nodeKey.getPubKey());

    }

    @Override
    public int hashCode () {
        return nodeKey != null ? nodeKey.hashCode() : 0;
    }

    public static NodeKey wrap (ECKey key) {
        return new NodeKey(key);
    }
}
