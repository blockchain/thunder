package network.thunder.core.communication.layer.middle.peerseed.messages;

import network.thunder.core.communication.layer.Message;

public abstract class PeerSeedMessage implements Message {
    @Override
    public String getMessageType () {
        return "PeerSeedMessage";
    }
}
