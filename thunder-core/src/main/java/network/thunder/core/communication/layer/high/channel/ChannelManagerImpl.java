package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.blockchain.ChannelFailureAction;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.communication.processor.implementations.management.BlockchainWatcher;
import network.thunder.core.communication.processor.implementations.management.ChannelBlockchainWatcher;
import network.thunder.core.communication.layer.high.Channel;

import java.util.HashMap;
import java.util.Map;

public class ChannelManagerImpl implements ChannelManager {

    BlockchainHelper blockchainHelper;

    Map<Channel, BlockchainWatcher> watcherMap = new HashMap<>();
    Map<Channel, Command> successMap = new HashMap<>();

    public ChannelManagerImpl (BlockchainHelper blockchainHelper) {
        this.blockchainHelper = blockchainHelper;
    }

    @Override
    public void onExchangeDone (Channel channel, Command successCommand) {
        BlockchainWatcher blockchainWatcher = new ChannelBlockchainWatcher(blockchainHelper, this, channel);
        watcherMap.put(channel, blockchainWatcher);
        successMap.put(channel, successCommand);

        blockchainWatcher.start();
    }

    @Override
    public void onAnchorDone (Channel channel) {
        successMap.get(channel).execute();

    }

    @Override
    public void onAnchorFailure (Channel channel, ChannelFailureAction failureAction) {

    }

    @Override
    public boolean queryChannelReady (Channel channel) {
        return false;
    }
}
