package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.ConnectionManager;
import network.thunder.core.communication.ConnectionRegistry;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.communication.processor.implementations.management.BlockchainWatcher;
import network.thunder.core.communication.processor.implementations.management.ChannelBlockchainWatcher;
import network.thunder.core.database.DBHandler;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.blockchain.ChannelFailureAction;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.Command;
import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;
import network.thunder.core.helper.callback.results.FailureResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChannelManagerImpl implements ChannelManager {

    ConnectionManager connectionManager;
    ConnectionRegistry connectionRegistry;
    BlockchainHelper blockchainHelper;
    DBHandler dbHandler;

    Map<Channel, BlockchainWatcher> watcherMap = new HashMap<>();
    Map<Channel, Command> successMap = new HashMap<>();

    Map<NodeKey, ChannelOpener> channelOpenerMap = new ConcurrentHashMap<>();
    Map<NodeKey, ChannelCloser> channelCloserMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ChannelManagerImpl (ContextFactory contextFactory, DBHandler dbHandler) {
        this.dbHandler = dbHandler;
        this.blockchainHelper = contextFactory.getBlockchainHelper();
        this.connectionManager = contextFactory.getConnectionManager();
        this.connectionRegistry = contextFactory.getConnectionRegistry();

        scheduler.scheduleAtFixedRate(new ChannelWatcherThread(), 10, 10, TimeUnit.SECONDS);
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
        //TODO
    }

    @Override
    public boolean queryChannelReady (Channel channel) {
        //TODO
        return false;
    }

    @Override
    public void onChannelClosed (Channel channel) {
        //TODO
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
            System.out.println("channelOpener = null?");
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
            if (!connectionRegistry.isConnected(node)) {
                connectionManager.connect(node, ConnectionIntent.MAINTAIN_CHANNEL, new ConnectionListener());
            }
        }
    }

}
