package network.thunder.core.etc;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelCloser;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.channel.ChannelOpener;
import network.thunder.core.helper.blockchain.ChannelFailureAction;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.helper.callback.ResultCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockChannelManager implements ChannelManager {
    Map<NodeKey, ChannelCloser> closeProcessorMap = new ConcurrentHashMap<>();

    @Override
    public void onExchangeDone (Channel channel, Command successCommand) {

    }

    @Override
    public void onAnchorDone (Channel channel) {

    }

    @Override
    public void onAnchorFailure (Channel channel, ChannelFailureAction failureAction) {

    }

    @Override
    public boolean queryChannelReady (Channel channel) {
        return false;
    }

    @Override
    public void onChannelClosed (Channel channel) {

    }

    @Override
    public void openChannel (NodeKey node, ChannelOpenListener channelOpenListener) {

    }

    @Override
    public void closeChannel (Channel channel, ResultCommand callback) {
        if (closeProcessorMap.containsKey(channel)) {
            closeProcessorMap.get(channel).closeChannel(channel, callback);
        }

    }

    @Override
    public void addChannelOpener (NodeKey node, ChannelOpener channelOpener) {

    }

    @Override
    public void removeChannelOpener (NodeKey node) {

    }

    @Override
    public void addChannelCloser (NodeKey node, ChannelCloser channelCloser) {

    }

    @Override
    public void removeChannelCloser (NodeKey node) {

    }
}
