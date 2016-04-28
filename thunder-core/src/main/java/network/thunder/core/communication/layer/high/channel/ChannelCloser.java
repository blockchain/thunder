package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.helper.callback.ResultCommand;

public interface ChannelCloser {
    void closeChannel (Channel channel, ResultCommand callback);
}
