package network.thunder.core.communication.layer.middle.broadcasting.gossip.messages;

import network.thunder.core.communication.layer.MesssageFactoryImpl;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;

import java.util.List;

public class GossipMessageFactoryImpl extends MesssageFactoryImpl implements GossipMessageFactory {
    @Override
    public GossipGetMessage getGossipGetMessage (List<byte[]> inventoryList) {
        return new GossipGetMessage(inventoryList);
    }

    @Override
    public GossipInvMessage getGossipInvMessage (List<byte[]> inventoryList) {
        return new GossipInvMessage(inventoryList);
    }

    @Override
    public GossipSendMessage getGossipSendMessage (List<P2PDataObject> objectList) {
        return new GossipSendMessage(objectList);
    }
}
