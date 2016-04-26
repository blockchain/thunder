package network.thunder.core.communication;

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
}
