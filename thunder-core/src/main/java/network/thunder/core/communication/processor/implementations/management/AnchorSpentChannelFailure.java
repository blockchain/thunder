package network.thunder.core.communication.processor.implementations.management;

import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.helper.blockchain.ChannelFailureAction;
import network.thunder.core.communication.layer.high.Channel;

public class AnchorSpentChannelFailure implements ChannelFailureAction {

    Channel channel;

    public AnchorSpentChannelFailure (Channel channel) {
        this.channel = channel;
    }

    @Override
    public void execute (ChannelManager manager, BlockchainHelper blockchainHelper) {
        //TODO implement logic to claim funds
    }
}
