package network.thunder.core.communication.layer.middle.broadcasting.gossip.messages;

import network.thunder.core.communication.layer.Message;

public abstract class Gossip implements Message {
    @Override
    public String getMessageType () {
        return "Gossip";
    }
}
