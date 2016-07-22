package network.thunder.core.communication.layer.high.channel.establish;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.channel.ChannelOpener;
import network.thunder.core.communication.layer.high.channel.establish.messages.*;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.BroadcastHelper;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.communication.processor.exceptions.LNEstablishException;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.results.FailureResult;
import network.thunder.core.helper.callback.results.SuccessResult;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.wallet.WalletHelper;
import org.bitcoinj.core.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static network.thunder.core.communication.layer.high.Channel.Phase.ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION;
import static network.thunder.core.communication.layer.high.Channel.Phase.OPEN;

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
 *  - Add various changes to the channel statusSender / updates to the database
 *  - Reload a half-done channel from database if the connection breaks down
 *
 * Currently we are exchanging 8 messages in total,
 *
 *      Alice           Bob
 *
 *        A     ->
 *              <-       A
 *        B     ->
 *              <-       B
 *        C     ->
 *              <-       C
 *          <broadcast>
 *          <wait conf>
 *        D     ->
 *              <-       D
 *
 *
 * ------------------------
 *  A:
 *  channelKeyServer;
 *  amountClient;
 *  amountServer;
 *  anchorTransaction;
 *  addressBytes;
 *  minConfirmationAnchor;
 *
 *   * Data about the first commitment to be able to refund if necessary
 *  revocationHash;
 *  feePerByte;
 *  csvDelay;
 * ------------------------
 *  B:
 *  channelKeyServer;
 *  amountClient;
 *  amountServer;
 *  anchorTransaction;
 *  addressBytes;
 *  minConfirmationAnchor;
 *
 *   * Data about the first commitment to be able to refund if necessary
 *  revocationHash;
 *  feePerByte;
 *  csvDelay;
 * -----------------------
 *  A:
 *  signatureCommitA
 * -----------------------
 *  B:
 *  signatureCommitB
 * -----------------------
 *  A:
 *  signatureAnchorA
 * -----------------------
 *  B:
 *  signatureAnchorB
 * -----------------------
 * TODO: Finish here..
 * -------------------
 *  anchorUnsignedA
 *  In:
 *  txInA1
 *  txInA2
 *  […]
 *
 *  Out:
 *  2-of-2
 *  Change A
 * -------------------
 *  anchorUnsignedB
 *  In:
 *  txInA1
 *  txInA2
 *  […]
 *  txInB1
 *  txInB2
 *  […]
 *
 *  Out:
 *  2-of-2
 *  Change A
 *  Change B
 * --------------------
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
    LNPaymentLogic paymentLogic;

    MessageExecutor messageExecutor;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean startedPeriodicBroadcasting = false;

    public EstablishProgress establishProgress;

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
        this.paymentLogic = contextFactory.getLNPaymentLogic();
    }

    @Override
    public void onInboundMessage (Message message) {
        try {
            consumeMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            node.resultCallback.execute(new FailureResult(e.getMessage()));
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
        setNode(node.nodeKey);
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
        List<Channel> openChannel = dbHandler.getOpenChannel(node.nodeKey);
        if (openChannel.size() > 0) {
            startScheduledBroadcasting();
            messageExecutor.sendNextLayerActive();
        }
    }

    private void consumeMessage (Message message) {
        try {
            if (message instanceof LNEstablishAMessage) {
                processMessageA((LNEstablishAMessage) message);
            } else if (message instanceof LNEstablishBMessage) {
                processMessageB((LNEstablishBMessage) message);
            } else if (message instanceof LNEstablishCMessage) {
                processMessageC((LNEstablishCMessage) message);
            } else if (message instanceof LNEstablishDMessage) {
                processMessageD(message);
            } else {
                throw new UnsupportedOperationException("Don't know this LNEstablish Message: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageExecutor.closeConnection();
        }
    }

    @Override
    public void openChannel (Channel channel, ChannelOpenListener callback) {
        //Only support one channel per connection for now..
        if (dbHandler.getOpenChannel(node.nodeKey).size() > 0) {
            System.out.println("LNEstablishProcessorImpl.openChannel - already connected!");
            callback.onSuccess.execute();
            return;
        }
        //TODO take values from channel object to choose opening values
        this.channelOpenListener = callback;

        prepareOpenChannel();
        this.establishProgress.channel.amountServer = getAmountForNewChannel();
        this.establishProgress.channel.amountClient = getAmountForNewChannel();
        this.establishProgress.channel.feePerByte = 3;
        this.establishProgress.weStartedExchange = true;
        sendEstablishMessageA();
    }

    private void prepareOpenChannel () {
        this.establishProgress = new EstablishProgress();
        this.establishProgress.channel = new Channel();
    }

    private void processMessageA (LNEstablishAMessage message) {
        //Received a fresh request to open a new channel
        //TODO make sure we don't overwrite anything important here..
        if (this.establishProgress == null) {
            prepareOpenChannel();
            this.establishProgress.weStartedExchange = false;
        }
        if (testProgressReceivingMessageAmount(0)) {
            //TODO test for validity of establish settings
            //TODO test if inputs are paying adequate fees and are paying from SegWit outputs
            this.establishProgress.channel.nodeKeyClient = node.nodeKey;
            message.saveToChannel(establishProgress.channel);
            establishProgress.messages.add(message);
            if (establishProgress.weStartedExchange) {
                establishProgress.channel.addAnchorOutputToAnchor();
                sendEstablishMessageB();
            } else {
                sendEstablishMessageA();
            }
        } else {
            throw new LNEstablishException("LNEstablishProcessorImpl.processMessageA error");
        }
    }

    private void processMessageB (LNEstablishBMessage message) {
        if (testProgressReceivingMessageAmount(2)) {
            if (!establishProgress.weStartedExchange) {
                establishProgress.channel.addAnchorOutputToAnchor();
            }

            message.saveToChannel(establishProgress.channel);

            Transaction channelTransaction = paymentLogic.getChannelTransaction(
                    establishProgress.channel.anchorTx.getOutput(0).getOutPointFor(),
                    establishProgress.channel
            );

            paymentLogic.checkSignatures(
                    establishProgress.channel, establishProgress.channel.keyClient,
                    establishProgress.channel.channelSignatures,
                    channelTransaction,

                    Collections.emptyList());

            establishProgress.messages.add(message);
            if (establishProgress.weStartedExchange) {
                sendEstablishMessageC();
            } else {
                sendEstablishMessageB();
            }
        } else {
            throw new LNEstablishException("LNEstablishProcessorImpl.processMessageB error");
        }
    }

    private void processMessageC (LNEstablishCMessage message) {

        if (testProgressReceivingMessageAmount(4)) {
            message.saveToChannel(establishProgress.channel);
            establishProgress.messages.add(message);

            //TODO obviously we should check if the anchor signatures are correct
            if (establishProgress.weStartedExchange) {
                onChannelEstablished();
            } else {
                sendEstablishMessageC();
                onChannelEstablished();
            }
        } else {
            throw new LNEstablishException("LNEstablishProcessorImpl.processMessageC error");
        }
    }

    //TODO: Send MessageD once we have enough confirmations
    private void processMessageD (Message message) {

    }

    private boolean testProgressReceivingMessageAmount (int amount) {
        return (establishProgress.weStartedExchange && establishProgress.messages.size() == (amount + 1))
                || (!establishProgress.weStartedExchange && establishProgress.messages.size() == amount);
    }

    private void onChannelEstablished () {
        establishProgress.channel.anchorTxHash = establishProgress.channel.anchorTx.getHash();
        establishProgress.channel.phase = ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION;
        dbHandler.insertChannel(establishProgress.channel);
        blockchainHelper.broadcastTransaction(establishProgress.channel.anchorTx);

//        channelManager.onExchangeDone(channel, this::onEnoughConfirmations);
        this.onEnoughConfirmations();

        if (channelOpenListener != null) {
            channelOpenListener.onStart(new SuccessResult());
        }
    }

    private void onEnoughConfirmations () {
        establishProgress.channel.phase = OPEN;

        dbHandler.updateChannelStatus(
                getNode(),
                establishProgress.channel.getHash(),
                serverObject.pubKeyServer,
                establishProgress.channel,
                null,
                null,
                null,
                null);

        sendNextLayerActiveIfOpenChannelExists();
        eventHelper.onChannelOpened(establishProgress.channel);

        if (channelOpenListener != null) {
            channelOpenListener.onFinished(new SuccessResult());
        }
    }

    private void sendEstablishMessageA () {
        establishProgress.channel.masterPrivateKeyServer = Tools.getRandomByte(20);
        establishProgress.channel.getAnchorTransactionServer(walletHelper);
        establishProgress.channel.revoHashServerCurrent = new RevocationHash(0, establishProgress.channel.masterPrivateKeyServer);
        establishProgress.channel.revoHashServerNext = new RevocationHash(1, establishProgress.channel.masterPrivateKeyServer);
        establishProgress.channel.addressServer = walletHelper.fetchAddress();
        LNEstablish message = messageFactory.getEstablishMessageA(establishProgress.channel);
        establishProgress.messages.add(message);
        messageExecutor.sendMessageUpwards(message);
    }

    private void sendEstablishMessageB () {

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                establishProgress.channel.anchorTx.getOutput(0).getOutPointFor(),
                establishProgress.channel.reverse()
        );

        establishProgress.channel.channelSignatures =
                paymentLogic.getSignatureObject(
                        establishProgress.channel, establishProgress.channel.keyServer,
                        channelTransaction,
                        Collections.emptyList());

        LNEstablish message = messageFactory.getEstablishMessageB(establishProgress.channel.channelSignatures.channelSignatures.get(0));
        establishProgress.messages.add(message);
        messageExecutor.sendMessageUpwards(message);
    }

    private void sendEstablishMessageC () {
        Transaction anchorTx = establishProgress.channel.anchorTx;
        anchorTx = walletHelper.signTransaction(anchorTx);
        establishProgress.channel.anchorTx = anchorTx;

        LNEstablish message = messageFactory.getEstablishMessageC(anchorTx);
        establishProgress.messages.add(message);
        messageExecutor.sendMessageUpwards(message);
    }

    private void broadcastChannelObject () {
        if (establishProgress == null || establishProgress.channel == null) {
            return;
        }
        Channel channel = establishProgress.channel;
        PubkeyChannelObject channelObject = new PubkeyChannelObject();
        channelObject.pubkeyA = serverObject.pubKeyServer.getPubKey();
        channelObject.pubkeyB = node.nodeKey.getPubKey();
        channelObject.pubkeyA1 = channel.keyServer.getPubKey();
        channelObject.pubkeyB1 = channel.keyClient.getPubKey();
        channelObject.timestamp = Tools.currentTime();
        channelObject.txidAnchor = channel.anchorTxHash.getBytes();

        //TODO fill in some usable data into ChannelStatusObject
        ChannelStatusObject statusObject = new ChannelStatusObject();
        statusObject.pubkeyA = serverObject.pubKeyServer.getPubKey();
        statusObject.pubkeyB = node.nodeKey.getPubKey();
        statusObject.timestamp = Tools.currentTime();
        statusObject.feeA = Fee.ZERO_FEE;
        statusObject.feeB = Fee.ZERO_FEE;

        broadcastHelper.broadcastNewObject(channelObject);
        broadcastHelper.broadcastNewObject(statusObject);
    }

    private long getAmountForNewChannel () {
        return (long) (walletHelper.getSpendableAmount() * PERCENTAGE_OF_FUNDS_PER_CHANNEL);
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
