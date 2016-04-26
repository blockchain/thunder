package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.helper.callback.ChannelOpenListener;

public interface ChannelOpener {
    void openChannel (Channel channel, ChannelOpenListener callback);
}
