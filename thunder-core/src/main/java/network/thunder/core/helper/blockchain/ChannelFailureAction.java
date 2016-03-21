package network.thunder.core.helper.blockchain;

import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.communication.layer.high.channel.ChannelManager;

public interface ChannelFailureAction {
    void execute (ChannelManager manager, BlockchainHelper blockchainHelper);
}
