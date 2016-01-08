package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipInvMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipSendMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface GossipMessageFactory extends MessageFactory {
    GossipGetMessage getGossipGetMessage (List<byte[]> inventoryList);

    GossipInvMessage getGossipInvMessage (List<byte[]> inventoryList);

    GossipSendMessage getGossipSendMessage (List<P2PDataObject> objectList);
}
