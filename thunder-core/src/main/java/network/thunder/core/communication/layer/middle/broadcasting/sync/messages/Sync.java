package network.thunder.core.communication.layer.middle.broadcasting.sync.messages;

import network.thunder.core.communication.layer.Message;

public abstract class Sync implements Message {
    @Override
    public String getMessageType () {
        return "Sync";
    }
}
