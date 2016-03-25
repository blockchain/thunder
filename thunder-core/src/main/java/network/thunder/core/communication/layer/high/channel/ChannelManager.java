package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.close.LNCloseProcessor;
import network.thunder.core.helper.blockchain.ChannelFailureAction;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.helper.callback.ResultCommand;

public interface ChannelManager {
    void onExchangeDone (Channel channel, Command successCommand);

    void onAnchorDone (Channel channel);

    void onAnchorFailure (Channel channel, ChannelFailureAction failureAction);

    boolean queryChannelReady (Channel channel);

    void onChannelClosed (Channel channel);

    void closeChannel (Channel channel, ResultCommand callback);

    void addCloseProcessor (LNCloseProcessor processor, Channel channel);

    void removeCloseProcessor (Channel channel);

}
