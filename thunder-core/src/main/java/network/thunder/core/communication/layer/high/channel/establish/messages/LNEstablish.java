package network.thunder.core.communication.layer.high.channel.establish.messages;

import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.high.Channel;

public abstract class LNEstablish implements Message {
    public abstract Channel saveToChannel (Channel channel);

    @Override
    public String getMessageType () {
        return "LNEstablish";
    }
}
