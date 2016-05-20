package network.thunder.core.communication.layer.high.channel.establish.messages;

import network.thunder.core.communication.layer.high.Channel;

public class LNEstablishDMessage implements LNEstablish {

    public LNEstablishDMessage () {
    }

    @Override
    public Channel saveToChannel (Channel channel) {
        return channel;
    }

    @Override
    public void verify () {
    }

    @Override
    public String toString () {
        return "LNEstablishDMessage{}";
    }
}
