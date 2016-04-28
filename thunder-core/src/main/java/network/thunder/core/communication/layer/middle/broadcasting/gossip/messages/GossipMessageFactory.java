package network.thunder.core.communication.layer.middle.broadcasting.gossip.messages;

import network.thunder.core.communication.layer.MessageFactory;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.messages.GossipGetMessage;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.messages.GossipInvMessage;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.messages.GossipSendMessage;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;

import java.util.List;

public interface GossipMessageFactory extends MessageFactory {
    GossipGetMessage getGossipGetMessage (List<byte[]> inventoryList);

    GossipInvMessage getGossipInvMessage (List<byte[]> inventoryList);

    GossipSendMessage getGossipSendMessage (List<P2PDataObject> objectList);
}
