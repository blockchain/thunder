package network.thunder.core.etc;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.channel.close.LNCloseProcessor;
import network.thunder.core.helper.blockchain.ChannelFailureAction;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.helper.callback.ResultCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockChannelManager implements ChannelManager {
    Map<Channel, LNCloseProcessor> closeProcessorMap = new ConcurrentHashMap<>();

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
    public void closeChannel (Channel channel, ResultCommand callback) {
        if (closeProcessorMap.containsKey(channel)) {
            closeProcessorMap.get(channel).closeChannel(channel.id, callback);
        }

    }

    @Override
    public void addCloseProcessor (LNCloseProcessor processor, Channel channel) {
        closeProcessorMap.put(channel, processor);
    }

    @Override
    public void removeCloseProcessor (Channel channel) {
        closeProcessorMap.remove(channel);
    }
}
