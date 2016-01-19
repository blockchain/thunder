package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipInvMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipSendMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.interfaces.factories.GossipMessageFactory;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
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
