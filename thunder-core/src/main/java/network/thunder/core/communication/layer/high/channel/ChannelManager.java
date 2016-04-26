package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.helper.blockchain.ChannelFailureAction;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.helper.callback.ResultCommand;

public interface ChannelManager {
    void onExchangeDone (Channel channel, Command successCommand);

    void onAnchorDone (Channel channel);

    void onAnchorFailure (Channel channel, ChannelFailureAction failureAction);

    boolean queryChannelReady (Channel channel);

    void onChannelClosed (Channel channel);

    void openChannel (NodeKey node, ChannelOpenListener channelOpenListener);
    void closeChannel (Channel channel, ResultCommand callback);

    void addChannelOpener (NodeKey node, ChannelOpener channelOpener);
    void removeChannelOpener (NodeKey node);
    void addChannelCloser (NodeKey node, ChannelCloser channelCloser);
    void removeChannelCloser (NodeKey node);

}
