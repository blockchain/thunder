package network.thunder.core.communication.layer;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.NodeKey;

public abstract class AuthenticatedProcessor extends Processor {
    NodeKey nodeKey;

    public void setNode (NodeKey node) {
        Preconditions.checkArgument(this.nodeKey == null);
        this.nodeKey = node;
    }

    public NodeKey getNode () {
        Preconditions.checkNotNull(nodeKey);
        return nodeKey;
    }

}
