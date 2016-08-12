package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.ConnectionManager;
import network.thunder.core.communication.ConnectionRegistry;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.communication.processor.implementations.management.BlockchainWatcher;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.ChannelSettlement;
import network.thunder.core.etc.BlockWrapper;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.ChainSettlementHelper;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;
import network.thunder.core.helper.callback.results.FailureResult;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static network.thunder.core.communication.layer.high.Channel.Phase.*;
import static network.thunder.core.communication.layer.high.channel.ChannelOpener.NullChannelOpener;
import static network.thunder.core.database.objects.ChannelSettlement.SettlementPhase.UNSETTLED;

public class ChannelManagerImpl implements ChannelManager {
    private static final Logger log = Tools.getLogger();

    ConnectionManager connectionManager;
    ConnectionRegistry connectionRegistry;
    BlockchainHelper blockchainHelper;
    DBHandler dbHandler;

    Map<Sha256Hash, BlockchainWatcher> watcherMap = new HashMap<>();
    Map<Sha256Hash, Command> successMap = new HashMap<>();

    Map<Sha256Hash, Lock> channelLockMap = new ConcurrentHashMap<>();

    Map<NodeKey, ChannelOpener> channelOpenerMap = new ConcurrentHashMap<>();
    Map<NodeKey, ChannelCloser> channelCloserMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ChannelManagerImpl (ContextFactory contextFactory, DBHandler dbHandler) {
        this.dbHandler = dbHandler;
        this.blockchainHelper = contextFactory.getBlockchainHelper();
        this.connectionManager = contextFactory.getConnectionManager();
        this.connectionRegistry = contextFactory.getConnectionRegistry();

    }

    @Override
    public void setup () {
        scheduler.scheduleAtFixedRate(new ChannelWatcherThread(), 1, 10, TimeUnit.SECONDS);
        List<Channel> channelList = dbHandler.getChannel();

        //Catch up with existing blocks
        List<BlockWrapper> blockList = blockchainHelper.getBlocksSince(dbHandler.getLastBlockHeight());
        List<Lock> lockList = channelList.stream().map(channel -> getChannelLock(channel.getHash())).collect(Collectors.toList());
        try {
            lockList.forEach(Lock::lock);

            for (Channel channel : channelList) {
                for (BlockWrapper blockWrapper : blockList) {

                    if (channel.phase == ESTABLISH_REQUESTED) {
                        if (blockWrapper.block.getTransactions().stream().map(Transaction::getHash).filter(channel.anchorTxHash::equals).count() > 0) {
                            channel.anchorBlockHeight = blockWrapper.height;
                            channel.phase = ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION;
                        }
                    }

                    if (channel.phase == ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION) {
                        int confirmations = blockWrapper.height - channel.anchorBlockHeight;
                        if (confirmations > channel.minConfirmationAnchor) {
                            channel.phase = OPEN;
                        }
                    }

                    ChainSettlementHelper.onBlock(blockWrapper, dbHandler, channel);
                    ChainSettlementHelper.onBlockSave(blockWrapper, dbHandler, channel);
                }
                //TODO directly take action once synced up through previous blocks if necessary and don't wait for the next block
            }

        } finally {
            lockList.forEach(Lock::unlock);
        }

        blockchainHelper.addBlockListener(blockWrapper -> {
            for (Channel channel : dbHandler.getChannel()) {
                getChannelLock(channel.getHash()).lock();
                if (channel.phase == ESTABLISH_REQUESTED) {
                    if (channel.minConfirmationAnchor == 0 ||
                            blockWrapper.block.getTransactions().stream().map(Transaction::getHash).filter(channel.anchorTxHash::equals).count() > 0) {
                        channel.anchorBlockHeight = blockWrapper.height;
                        channel.phase = ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION;
                    }
                }

                if (channel.phase == ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION) {
                    int confirmations = blockWrapper.height - channel.anchorBlockHeight;
                    if (confirmations >= channel.minConfirmationAnchor) {
                        channel.phase = OPEN;
                        channelOpenerMap.getOrDefault(channel.nodeKeyClient, new NullChannelOpener()).onAnchorConfirmed(channel.getHash());
                    }
                }

                ChainSettlementHelper.onBlock(blockWrapper, dbHandler, channel);
                ChainSettlementHelper.onBlockSave(blockWrapper, dbHandler, channel);
                List<ChannelSettlement> settlements = dbHandler.getSettlements(channel.getHash());
                for (ChannelSettlement settlement : settlements) {
                    if (settlement.phase == UNSETTLED && settlement.timeToSettle <= blockWrapper.height) {
                        ChainSettlementHelper.onBlockAction(
                                blockchainHelper,
                                channel,
                                settlement
                        );
                    }
                }
                getChannelLock(channel.getHash()).unlock();
            }
        });

    }

    @Override
    public Lock getChannelLock (Sha256Hash channelHash) {
        channelLockMap.putIfAbsent(channelHash, new ReentrantLock());
        return channelLockMap.get(channelHash);
    }

    @Override
    public void openChannel (NodeKey node, ChannelOpenListener channelOpenListener) {
        connectionManager.connect(node, ConnectionIntent.OPEN_CHANNEL,
                new ConnectionListener()
                        .setOnSuccess(() -> openChannelWithOpenConnection(node, channelOpenListener))
                        .setOnFailure(() -> channelOpenListener.onFinished(new FailureResult())));
    }

    private void openChannelWithOpenConnection (NodeKey node, ChannelOpenListener channelOpenListener) {
        ChannelOpener channelOpener = channelOpenerMap.get(node);
        if (channelOpener != null) {

            //TODO Pass on some values for the channel once they are used
            channelOpener.openChannel(null, channelOpenListener);
        } else {
            //Should never happen, the call to addChannelOpener happens before this one here..
            log.error("channelOpener = null?");
        }
    }

    @Override
    public void closeChannel (Channel channel, ResultCommand callback) {

        NodeKey node = channel.nodeKeyClient;
        ChannelCloser channelCloser = channelCloserMap.get(node);

        if (channelCloser != null) {
            channelCloser.closeChannel(channel, callback);
        } else {
            //TODO we currently aren't connected with a node that we have a channel open. We have a worker thread that automatically tries to
            //reconnect, so don't have to call connect here, but should somehow mark this channel as to-be-closed..
        }
    }

    @Override
    public void addChannelOpener (NodeKey node, ChannelOpener channelOpener) {
        this.channelOpenerMap.put(node, channelOpener);
    }

    @Override
    public void removeChannelOpener (NodeKey node) {
        this.channelOpenerMap.remove(node);
    }

    @Override
    public void addChannelCloser (NodeKey node, ChannelCloser channelCloser) {
        this.channelCloserMap.put(node, channelCloser);
    }

    @Override
    public void removeChannelCloser (NodeKey node) {
        this.channelCloserMap.remove(node);
    }

    private class ChannelWatcherThread implements Runnable {
        @Override
        public void run () {
            maintainChannel();
        }
    }

    private void maintainChannel () {
        //Do all kind of maintenance in here, like reconnecting to channels that disconnected..
        List<Channel> openChannel = dbHandler.getOpenChannel();
        for (Channel channel : openChannel) {
            NodeKey node = channel.nodeKeyClient;
            log.debug("ChannelManagerImpl.maintainChannel open channel "+channel+" with "+node+" is "+ connectionRegistry.isConnected(node)+" connected..");
            if (!connectionRegistry.isConnected(node)) {
                connectionManager.connect(node, ConnectionIntent.MAINTAIN_CHANNEL, new ConnectionListener());
            }
        }
    }

}
