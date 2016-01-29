package network.thunder.core.communication.processor.implementations.gossip;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;

/**
 * Created by matsjerratsch on 29/01/2016.
 */
public interface BroadcastHelper {

    void broadcastNewObject (P2PDataObject object);
}
