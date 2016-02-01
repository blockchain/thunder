package network.thunder.core.etc;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.processor.implementations.gossip.BroadcastHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 01/02/2016.
 */
public class MockBroadcastHelper implements BroadcastHelper {

    public List<P2PDataObject> broadcastedObjects = new ArrayList<>();

    @Override
    public void broadcastNewObject (P2PDataObject object) {
        broadcastedObjects.add(object);
    }
}
