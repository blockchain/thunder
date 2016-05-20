package network.thunder.core.communication.layer.high.channel.establish;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.establish.messages.LNEstablish;

import java.util.ArrayList;
import java.util.List;

public class EstablishProgress {
    boolean weStartedExchange = false;
    List<LNEstablish> messages = new ArrayList<>();
    public Channel channel;

    public long countMessage (Class c) {
        return messages.stream().filter(o -> o.getClass() == c).count();
    }
}
