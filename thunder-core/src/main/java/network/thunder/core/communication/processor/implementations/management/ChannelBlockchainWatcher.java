package network.thunder.core.communication.processor.implementations.management;

import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.helper.blockchain.OnBlockCommand;
import network.thunder.core.helper.blockchain.OnTxCommand;
import network.thunder.core.communication.layer.high.channel.establish.LNEstablishProcessorImpl;
import network.thunder.core.communication.layer.high.channel.establish.LNEstablishProcessor;
import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;

public class ChannelBlockchainWatcher extends BlockchainWatcher {

    private Channel channel;
    private ChannelManager channelManager;

    private boolean started;
    private boolean stopped;

    boolean seen = false;
    boolean confirmed = false;
    boolean anchorDone = false;
    int confirmations = 0;
    int blockSince = 0;

    OnTxCommand anchorSuccess;
    OnTxCommand anchorEscape;
    OnBlockCommand blockCommand;

    public ChannelBlockchainWatcher (BlockchainHelper blockchainHelper, ChannelManager channelManager, Channel channel) {
        super(blockchainHelper);
        this.channel = channel;
        this.channelManager = channelManager;
    }

    @Override
    public void start () {

        Transaction tx = blockchainHelper.getTransaction(channel.anchorTxHashClient);
        if (tx != null) {
            int depth = tx.getConfidence().getDepthInBlocks();
            if (depth > 0) {
                confirmed = true;
                confirmations = depth;
            }
        }

        anchorSuccess = new OnTxCommand() {
            @Override
            public boolean compare (Transaction tx) {
                if (stopped) {
                    return true;
                } else {
                    return channel.getAnchorTxHashClient().equals(tx.getHash());
                }
            }

            @Override
            public void execute (Transaction tx) {
                if (stopped) {
                    return;
                } else if (LNEstablishProcessorImpl.MIN_CONFIRMATIONS == 0) {
                    channelManager.onAnchorDone(channel);
                }
            }
        };

        anchorEscape = new OnTxCommand() {
            @Override
            public boolean compare (Transaction tx) {
                if (stopped) {
                    return true;
                }
                for (TransactionInput input : tx.getInputs()) {
                    if (input.getOutpoint().getHash().equals(channel.anchorTxHashClient) ||
                            input.getOutpoint().getHash().equals(channel.anchorTxHashServer)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void execute (Transaction tx) {
                if (stopped) {
                    return;
                } else {
                    channelManager.onAnchorFailure(channel, new AnchorSpentChannelFailure(channel));
                }
            }
        };

        blockCommand = block -> {
            if (stopped) {
                return true;
            }
            if (!confirmed) {
                if (block.getTransactions().contains(tx)) {
                    confirmed = true;
                }
            }

            if (confirmed) {
                confirmations++;
            } else {
                blockSince++;
            }

            if (confirmations >= LNEstablishProcessorImpl.MIN_CONFIRMATIONS) {
                channelManager.onAnchorDone(channel);
                return true;
            } else {
                if ((blockSince > LNEstablishProcessor.MAX_WAIT_FOR_OTHER_TX_IF_SEEN && seen) ||
                        (blockSince > LNEstablishProcessor.MAX_WAIT_FOR_OTHER_TX && !seen)) {
                    channelManager.onAnchorFailure(channel, new AnchorNotFoundChannelFailure(channel));
                }
            }

            return false;
        };

        blockchainHelper.addBlockListener(this.blockCommand);
        blockchainHelper.addTxListener(this.anchorEscape);
        blockchainHelper.addTxListener(this.anchorSuccess);

    }

    @Override
    public void stop () {

    }
}
