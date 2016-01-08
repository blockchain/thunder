package network.thunder.core.communication.processor.implementations.gossip;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 01/12/2015.
 */
public interface NodeObserver {
    void update ();

    void update (List<P2PDataObject> objectList);

}
