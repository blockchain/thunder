package network.thunder.core.communication.processor.implementations.management;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.channel.establish.LNEstablishProcessor;
import network.thunder.core.communication.layer.high.channel.establish.LNEstablishProcessorImpl;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.ChannelSettlement;
import network.thunder.core.helper.ChainSettlementHelper;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.blockchain.OnBlockCommand;
import network.thunder.core.helper.blockchain.OnTxCommand;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;

import java.util.List;

import static network.thunder.core.database.objects.ChannelSettlement.SettlementPhase.UNSETTLED;

public class ChannelBlockchainWatcher extends BlockchainWatcher {

    private Sha256Hash channelHash;
    private Sha256Hash anchorHash;
    private ChannelManager channelManager;

    private DBHandler dbHandler;
    private LNPaymentLogic paymentLogic;

    private boolean stopped = false;

    boolean seen = false;
    boolean confirming = false;
    boolean anchorDone = false;
    int confirmations = 0;
    int blockSince = 0;

    OnTxCommand anchorSuccess;
    OnTxCommand anchorEscape;
    OnBlockCommand blockCommand;

    public ChannelBlockchainWatcher (BlockchainHelper blockchainHelper, ChannelManager channelManager, Channel channel) {
        super(blockchainHelper);
        this.channelHash = channel.getHash();
        this.channelManager = channelManager;
        this.anchorHash = channel.anchorTxHash;
    }

    public ChannelBlockchainWatcher (
            BlockchainHelper blockchainHelper,
            ChannelManager channelManager,
            Channel channel,
            DBHandler dbHandler,
            LNPaymentLogic paymentLogic) {
        super(blockchainHelper);
        this.channelHash = channel.getHash();
        this.channelManager = channelManager;
        this.anchorHash = channel.anchorTxHash;
        this.dbHandler = dbHandler;
        this.paymentLogic = paymentLogic;
    }

    @Override
    public void start () {

        Transaction anchor = blockchainHelper.getTransaction(anchorHash);
        if (anchor != null) {
            int depth = anchor.getConfidence().getDepthInBlocks();
            if (depth > 0) {
                confirming = true;
                confirmations = depth;
            }
        }

        anchorSuccess = new OnTxCommand() {
            @Override
            public boolean compare (Transaction tx) {
                return stopped || anchorHash.equals(tx.getHash());
            }

            @Override
            public void execute (Transaction tx) {
                if (LNEstablishProcessorImpl.MIN_CONFIRMATIONS == 0) {
                    channelManager.onAnchorDone(getChannel());
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
                    if (input.getOutpoint().getHash().equals(anchorHash) && input.getOutpoint().getIndex() == 0) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void execute (Transaction tx) {
                //We just save that this channel got closed somehow here..
                Channel channel = getChannel();
                if (channel.phase != Channel.Phase.CLOSE_ON_CHAIN) {
                    channel.phase = Channel.Phase.CLOSE_ON_CHAIN;
                    saveChannel(channel);
                }
                channelManager.onAnchorFailure(channel, new AnchorSpentChannelFailure(channel));
            }
        };

        blockCommand = block -> {
            if (stopped) {
                return true;
            }
            Channel channel = getChannel();

            if (!anchorDone) {
                if (!confirming) {
                    if (block.getTransactions().contains(anchor)) {
                        confirming = true;
                    }
                }

                if (confirming) {
                    confirmations++;
                } else {
                    blockSince++;
                }

                if (confirmations >= LNEstablishProcessorImpl.MIN_CONFIRMATIONS) {
                    anchorDone = true;
                    channelManager.onAnchorDone(channel);
                } else {
                    if ((blockSince > LNEstablishProcessor.MAX_WAIT_FOR_OTHER_TX_IF_SEEN && seen) ||
                            (blockSince > LNEstablishProcessor.MAX_WAIT_FOR_OTHER_TX && !seen)) {
                        channelManager.onAnchorFailure(channel, new AnchorNotFoundChannelFailure(channel));
                    }
                }
            }

            //Go through the transactions of this block and see if any of it spends a channel.
            //Create settlements as we do so and save them in the database with the correct information
            for (Transaction transaction : block.getTransactions()) {
                for (TransactionInput input : transaction.getInputs()) {
                    if (input.getOutpoint().getHash().equals(anchorHash) && input.getOutpoint().getIndex() == 0) {
                        ChainSettlementHelper.onChannelTransaction(
                                blockchainHelper.getHeight(),
                                transaction,
                                dbHandler,
                                channel);
                    }
                }
            }

            if (channel.spendingTx != null) {
                List<ChannelSettlement> settlements = dbHandler.getSettlements(channelHash);
                for (ChannelSettlement settlement : settlements) {
                    ChainSettlementHelper.onBlockSave(
                            block,
                            blockchainHelper.getHeight(),
                            dbHandler,
                            channel,
                            settlement
                    );
                }

                for (ChannelSettlement settlement : settlements) {
                    if (settlement.phase == UNSETTLED && settlement.timeToSettle <= blockchainHelper.getHeight()) {
                        ChainSettlementHelper.onBlockAction(
                                blockchainHelper,
                                channel,
                                settlement
                        );
                    }
                }
            }

            return false;
        };

        blockchainHelper.addBlockListener(this.blockCommand);
        blockchainHelper.addTxListener(this.anchorEscape);
        blockchainHelper.addTxListener(this.anchorSuccess);

    }

    private Channel getChannel () {
        return dbHandler.getChannel(channelHash);
    }

    private void saveChannel (Channel channel) {
        this.dbHandler.updateChannel(channel);
    }

    @Override
    public void stop () {
        stopped = true;
    }
}
