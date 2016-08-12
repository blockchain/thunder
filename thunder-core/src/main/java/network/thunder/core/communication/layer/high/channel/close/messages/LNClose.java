package network.thunder.core.communication.layer.high.channel.close.messages;

import network.thunder.core.communication.layer.Message;

public abstract class LNClose implements Message {
    @Override
    public String getMessageType () {
        return "LNClose";
    }
}
