package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.helper.blockchain.ChannelFailureAction;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.communication.layer.high.Channel;

public interface ChannelManager {
    void onExchangeDone (Channel channel, Command successCommand);

    void onAnchorDone (Channel channel);

    void onAnchorFailure (Channel channel, ChannelFailureAction failureAction);

    boolean queryChannelReady (Channel channel);

}
