package network.thunder.core.communication.layer.high.channel.establish.messages;

import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.high.Channel;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public interface LNEstablish extends Message {
    public void saveToChannel (Channel channel);
}
