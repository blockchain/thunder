package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.interfaces.message.gossip.types.GossipGetMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.gossip.types.GossipInvMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.gossip.types.GossipSendMessage;
import network.thunder.core.communication.objects.p2p.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface GossipMessageFactory extends MessageFactory {
    GossipGetMessage getGossipGetMessage (List<byte[]> inventoryList);

    GossipInvMessage getGossipInvMessage (List<byte[]> inventoryList);

    GossipSendMessage getGossipSendMessage (List<P2PDataObject> objectList);
}
