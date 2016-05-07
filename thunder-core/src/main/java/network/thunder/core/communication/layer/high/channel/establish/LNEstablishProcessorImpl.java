package network.thunder.core.communication.layer.high.channel.establish;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.channel.ChannelOpener;
import network.thunder.core.communication.layer.high.channel.establish.messages.*;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.BroadcastHelper;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.results.ChannelCreatedResult;
import network.thunder.core.helper.callback.results.SuccessResult;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.wallet.WalletHelper;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 * Processor handling the creation of a channel.
 *
 * Open TODOS:
 *  - Exchange more general information like min confirmation, fees, amounts, ...
 *  - Rethink the anchor design, implementing SegWit
 *    - We can use a single tx with inputs from both parties
 *    - The problem with it is that we need a way to determine the value of the UTXOs
 *  - Have a anchor_complete message
 *  - Check after X confirmation for an anchor_complete, close and free our anchor again
 *  - Add various changes to the channel status / updates to the database
 *  - Reload a half-done channel from database if the connection breaks down
 *
 * Currently we are exchanging 4 messages,
 *
 *      Alice           Bob
 *
 *        A     ->
 *              <-       B
 *        C     ->
 *              <-       D
 *
 * whereas receiving message C or D completes the process of creating the channel.
 *
 * Upon completion, we broadcast the transaction to the p2p network and listen for sufficient confirmations.
 */
public class LNEstablishProcessorImpl extends LNEstablishProcessor implements ChannelOpener {
    public static final double PERCENTAGE_OF_FUNDS_PER_CHANNEL = 0.1;

    WalletHelper walletHelper;
    LNEstablishMessageFactory messageFactory;
    BroadcastHelper broadcastHelper;
    LNEventHelper eventHelper;
    DBHandler dbHandler;
    ClientObject node;
    ServerObject serverObject;
    BlockchainHelper blockchainHelper;
    ChannelManager channelManager;

    MessageExecutor messageExecutor;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean startedPeriodicBroadcasting = false;

    public Channel channel;
    int status = 0;

    ChannelOpenListener channelOpenListener;

    public LNEstablishProcessorImpl (ContextFactory contextFactory, DBHandler dbHandler, ClientObject node) {
        this.walletHelper = contextFactory.getWalletHelper();
        this.messageFactory = contextFactory.getLNEstablishMessageFactory();
        this.broadcastHelper = contextFactory.getBroadcastHelper();
        this.eventHelper = contextFactory.getEventHelper();
        this.dbHandler = dbHandler;
        this.node = node;
        this.serverObject = contextFactory.getServerSettings();
        this.blockchainHelper = contextFactory.getBlockchainHelper();
        this.channelManager = contextFactory.getChannelManager();
    }

    @Override
    public void onInboundMessage (Message message) {
        try {
            consumeMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            messageExecutor.sendMessageUpwards(messageFactory.getFailureMessage(e.getMessage()));
            node.resultCallback.execute(new ChannelCreatedResult(channel));
            throw e;
        }
    }

    @Override
    public boolean consumesInboundMessage (Object object) {
        return object instanceof LNEstablish;
    }

    @Override
    public boolean consumesOutboundMessage (Object object) {
        return false;
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        setNode(new NodeKey(node.pubKeyClient));
        this.messageExecutor = messageExecutor;
        channelManager.addChannelOpener(getNode(), this);
        sendNextLayerActiveIfOpenChannelExists();
    }

    @Override
    public void onLayerClose () {
        channelManager.removeChannelOpener(getNode());
        scheduler.shutdown();
    }

    private void sendNextLayerActiveIfOpenChannelExists () {
        List<Channel> openChannel = dbHandler.getOpenChannel(node.pubKeyClient);
        if (openChannel.size() > 0) {
            startScheduledBroadcasting();
            messageExecutor.sendNextLayerActive();
        }
    }

    private void consumeMessage (Message message) {
        if (message instanceof LNEstablishAMessage) {
            processMessageA(message);
        } else if (message instanceof LNEstablishBMessage) {
            processMessageB(message);
        } else if (message instanceof LNEstablishCMessage) {
            processMessageC(message);
        } else if (message instanceof LNEstablishDMessage) {
            processMessageD(message);
        } else {
            throw new UnsupportedOperationException("Don't know this LNEstablish Message: " + message);
        }
    }

    @Override
    public void openChannel (Channel channel, ChannelOpenListener callback) {
        //TODO take values from channel object to choose opening values
        this.channelOpenListener = callback;
        sendEstablishMessageA();
    }

    private void processMessageA (Message message) {
        checkStatus(0);
        LNEstablish m = (LNEstablish) message;
        prepareNewChannel();
        m.saveToChannel(channel);
        sendEstablishMessageB();
    }

    private void processMessageB (Message message) {
        checkStatus(2);
        LNEstablish m = (LNEstablish) message;
        m.saveToChannel(channel);
        sendEstablishMessageC();
    }

    private void processMessageC (Message message) {
        checkStatus(3);
        LNEstablish m = (LNEstablish) message;
        m.saveToChannel(channel);
        channel.verifyEscapeSignatures();
        sendEstablishMessageD();
        onChannelEstablished();
    }

    private void processMessageD (Message message) {
        checkStatus(4);
        LNEstablish m = (LNEstablish) message;
        m.saveToChannel(channel);
        channel.verifyEscapeSignatures();
        onChannelEstablished();
    }

    private void onChannelEstablished () {
        channel.isReady = true;
        dbHandler.saveChannel(channel);
//        channelManager.onExchangeDone(channel, this::onEnoughConfirmations);
        this.onEnoughConfirmations();
        blockchainHelper.broadcastTransaction(channel.getAnchorTransactionServer());

        if (channelOpenListener != null) {
            channelOpenListener.onStart(new SuccessResult());
        }
    }

    private void onEnoughConfirmations () {
        channel.initiateChannelStatus(serverObject.configuration);
        dbHandler.updateChannel(channel);
        sendNextLayerActiveIfOpenChannelExists();
        eventHelper.onChannelOpened(channel);

        if (channelOpenListener != null) {
            channelOpenListener.onFinished(new SuccessResult());
        }
    }

    private void sendEstablishMessageA () {
        prepareNewChannel();
        Message message = messageFactory.getEstablishMessageA(channel);
        messageExecutor.sendMessageUpwards(message);
        status = 2;
    }

    private void sendEstablishMessageB () {
        Transaction anchor = channel.getAnchorTransactionServer(walletHelper);
        Message message = messageFactory.getEstablishMessageB(channel, anchor);
        messageExecutor.sendMessageUpwards(message);
        status = 3;
    }

    private void sendEstablishMessageC () {
        Transaction anchor = channel.getAnchorTransactionServer(walletHelper);
        Transaction escape = channel.getEscapeTransactionClient();
        Transaction fastEscape = channel.getFastEscapeTransactionClient();

        TransactionSignature escapeSig = Tools.getSignature(escape, 0, channel.getScriptAnchorOutputClient().getProgram(), channel.getKeyServerA());
        TransactionSignature fastEscapeSig = Tools.getSignature(fastEscape, 0, channel.getScriptAnchorOutputClient().getProgram(), channel
                .getKeyServerA());

        Message message = messageFactory.getEstablishMessageC(anchor, escapeSig, fastEscapeSig);
        messageExecutor.sendMessageUpwards(message);

        status = 4;
    }

    private void sendEstablishMessageD () {
        Transaction escape = channel.getEscapeTransactionClient();
        Transaction fastEscape = channel.getFastEscapeTransactionClient();
        TransactionSignature escapeSig = Tools.getSignature(escape, 0, channel.getScriptAnchorOutputClient().getProgram(), channel.getKeyServerA());
        TransactionSignature fastEscapeSig = Tools.getSignature(fastEscape, 0, channel.getScriptAnchorOutputClient().getProgram(), channel
                .getKeyServerA());

        Message message = messageFactory.getEstablishMessageD(escapeSig, fastEscapeSig);
        messageExecutor.sendMessageUpwards(message);
        status = 5;
    }

    private void prepareNewChannel () {
        channel = new Channel(node.pubKeyClient.getPubKey(), serverObject.pubKeyServer, getAmountForNewChannel());
        channel.addressServer = walletHelper.fetchAddress();
        status = 1;
    }

    private void broadcastChannelObject () {
        if (channel == null) {
            return;
        }
        PubkeyChannelObject channelObject = new PubkeyChannelObject();
        channelObject.pubkeyA = serverObject.pubKeyServer.getPubKey();
        channelObject.pubkeyB = node.pubKeyClient.getPubKey();
        channelObject.pubkeyA1 = channel.keyServer.getPubKey();
        channelObject.pubkeyA2 = channel.keyServerA.getPubKey();
        channelObject.pubkeyB1 = channel.keyClient.getPubKey();
        channelObject.pubkeyB2 = channel.keyClientA.getPubKey();
        channelObject.timestamp = Tools.currentTime();
        channelObject.secretAHash = channel.anchorSecretHashServer;
        channelObject.secretBHash = channel.anchorSecretHashClient;
        channelObject.txidAnchor = channel.anchorTxHashServer.getBytes();

        //TODO fill in some usable data into ChannelStatusObject
        ChannelStatusObject statusObject = new ChannelStatusObject();
        statusObject.pubkeyA = serverObject.pubKeyServer.getPubKey();
        statusObject.pubkeyB = node.pubKeyClient.getPubKey();
        statusObject.timestamp = Tools.currentTime();

        broadcastHelper.broadcastNewObject(channelObject);
        broadcastHelper.broadcastNewObject(statusObject);
    }

    private long getAmountForNewChannel () {
        return (long) (walletHelper.getSpendableAmount() * PERCENTAGE_OF_FUNDS_PER_CHANNEL);
    }

    private void checkStatus (int expected) {
        if (status != expected) {
            messageExecutor.closeConnection();
            System.out.println("Status not correct.. Is: " + status + " Expected: " + expected);
        }
    }

    private synchronized void startScheduledBroadcasting () {
        if (!startedPeriodicBroadcasting) {
            startedPeriodicBroadcasting = true;
            broadcastChannelObject();
            int time = (int) (P2PDataObject.MAXIMUM_AGE_SYNC_DATA * 0.75);
            scheduler.scheduleAtFixedRate((Runnable) () -> broadcastChannelObject(), Tools.getRandom(0, time), time, TimeUnit.SECONDS);
        }
    }

}
