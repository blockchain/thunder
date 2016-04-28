package network.thunder.core.etc;

import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.BroadcastHelper;

import java.util.ArrayList;
import java.util.List;

public class MockBroadcastHelper implements BroadcastHelper {

    public List<P2PDataObject> broadcastedObjects = new ArrayList<>();

    @Override
    public void broadcastNewObject (P2PDataObject object) {
        broadcastedObjects.add(object);
    }
}
