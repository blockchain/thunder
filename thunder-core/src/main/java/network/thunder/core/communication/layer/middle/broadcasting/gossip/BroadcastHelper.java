package network.thunder.core.communication.layer.middle.broadcasting.gossip;

import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;

public interface BroadcastHelper {

    void broadcastNewObject (P2PDataObject object);
}
