package network.thunder.core.communication.layer.middle.broadcasting.gossip;

import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;

/**
 * Created by matsjerratsch on 29/01/2016.
 */
public interface BroadcastHelper {

    void broadcastNewObject (P2PDataObject object);
}
