package network.thunder.core.communication.processor.implementations.management;

import network.thunder.core.helper.blockchain.BlockchainHelper;

public abstract class BlockchainWatcher {
    BlockchainHelper blockchainHelper;

    public BlockchainWatcher (BlockchainHelper blockchainHelper) {
        this.blockchainHelper = blockchainHelper;
    }

    public abstract void start ();

    public abstract void stop ();
}
