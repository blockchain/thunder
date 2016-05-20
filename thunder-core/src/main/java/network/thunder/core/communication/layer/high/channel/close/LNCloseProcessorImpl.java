package network.thunder.core.communication.layer.high.channel.close;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.channel.ChannelCloser;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.channel.close.messages.LNClose;
import network.thunder.core.communication.layer.high.channel.close.messages.LNCloseAMessage;
import network.thunder.core.communication.layer.high.channel.close.messages.LNCloseMessageFactory;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.ScriptTools;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.callback.ResultCommand;
import network.thunder.core.helper.callback.results.NullResultCommand;
import network.thunder.core.helper.callback.results.SuccessResult;
import network.thunder.core.helper.events.LNEventHelper;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static network.thunder.core.communication.layer.high.Channel.Phase.CLOSE_REQUESTED_CLIENT;
import static network.thunder.core.communication.layer.high.Channel.Phase.CLOSE_REQUESTED_SERVER;

public class LNCloseProcessorImpl extends LNCloseProcessor implements ChannelCloser {

    LNEventHelper eventHelper;
    DBHandler dbHandler;
    ClientObject node;
    ServerObject serverObject;
    BlockchainHelper blockchainHelper;
    ChannelManager channelManager;

    MessageExecutor messageExecutor;

    LNCloseMessageFactory messageFactory;

    ResultCommand callback = new NullResultCommand();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Channel channel;

    boolean isBlocked = false;
    boolean weRequestedClose = false;

    Sha256Hash channelHashToClose;

    public LNCloseProcessorImpl (ContextFactory contextFactory, DBHandler dbHandler, ClientObject node) {
        this.messageFactory = contextFactory.getLNCloseMessageFactory();
        this.eventHelper = contextFactory.getEventHelper();
        this.dbHandler = dbHandler;
        this.node = node;
        this.serverObject = contextFactory.getServerSettings();
        this.blockchainHelper = contextFactory.getBlockchainHelper();
        this.channelManager = contextFactory.getChannelManager();
    }

    public static ChannelStatus getChannelStatus (ChannelStatus channelStatus) {
        //We don't want any open payments showing up in the closing transaction.
        //This means we deliberately give up any payments that are still open.
        //The underlying control is responsible to display a warning about it to the user.

        //There are two options on how to handle the open payments. Because one party wants to close the channel, that party should
        //give up any outstanding rights on open payments. It cannot demand that the other party gives out free money for not-redeemed payments.
        //If there are big outstanding payments, one should block the channel for new payments and just wait for the old payments to settle
        //TODO allow blocking a channel for new payments to wait for old payments to settle

        //Here we don't care about the user interest, we assume the underlying interface warned the users of the consequences.
        //We just refund all open receiving payments and settle all open sent payments, even though this will probably cost us money.

        //Rather use a copy here to not run into concurrency issues somewhere else..
        ChannelStatus temp = channelStatus.getClone();

        long amountClient = temp.amountClient;
        long amountServer = temp.amountServer;

        for (PaymentData paymentData : temp.paymentList) {
            amountClient += paymentData.amount;
        }

        temp.paymentList.clear();
        temp.amountClient = amountClient;
        temp.amountServer = amountServer;

        return temp;
    }

    public Transaction getClosingTransaction (ChannelStatus channelStatus, float feePerByte) {
        //For the sake of privacy (and simplicity) we use lexicographically ordering here, as defined in BIP69
        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addInput(channel.anchorTxHash, 0, Tools.getDummyScript());

        //TODO deduct the transaction fee correctly from both amounts
        //TODO would be better to have another address on file that we can use here..
        long feePerParty = (Tools.getTransactionFees(2, 2, feePerByte) / 2);
        transaction.addOutput(Coin.valueOf(channelStatus.amountClient - feePerParty), channel.channelStatus.addressClient);
        transaction.addOutput(Coin.valueOf(channelStatus.amountServer - feePerParty), channel.channelStatus.addressServer);
        return Tools.applyBIP69(transaction);
    }

    private boolean checkFee (LNCloseAMessage message) {
        return message.feePerByte > serverObject.configuration.MIN_FEE_PER_BYTE_CLOSING &&
                message.feePerByte < serverObject.configuration.MAX_FEE_PER_BYTE_CLOSING;
    }

    private List<TransactionSignature> getTransactionSignatures (Transaction transaction) {
        return Tools.getChannelSignatures(channel, transaction);
    }

    @Override
    public void closeChannel (Channel channel, ResultCommand callback) {
        //Before we do anything, make sure we lock the channel and don't accept any new payments anymore..
        //TODO lock the channel
        this.callback = callback;
        this.channelHashToClose = channel.getHash();
        this.channel = getChannel();
        isBlocked = true;
        weRequestedClose = true;
        channel.phase = CLOSE_REQUESTED_SERVER;
        channel.isReady = false;
        dbHandler.updateChannel(channel);

        calculateAndSendCloseMessage();

        //TODO add some scheduler to check if we got an response within X minutes, do something about it
        //      if the other party is just plain unresponsive
    }

    private void calculateAndSendCloseMessage () {
        Channel channel = getChannel();
        ChannelStatus closingStatus = getChannelStatus(channel.channelStatus);

        //This gets called as well when the connection breaks down and we connect back to the other party
        //For now we take it that we may negotiate a close with a different fee
        Transaction closingTransaction = getClosingTransaction(closingStatus, serverObject.configuration.DEFAULT_FEE_PER_BYTE_CLOSING);
        sendCloseMessage(getTransactionSignatures(closingTransaction));
    }

    private void sendCloseMessage (List<TransactionSignature> signatureList) {
        LNClose close = messageFactory.getLNCloseAMessage(channelHashToClose, signatureList, serverObject.configuration.DEFAULT_FEE_PER_BYTE_CLOSING);
        messageExecutor.sendMessageUpwards(close);

    }

    private Channel getChannel () {
        //TODO quick hack here - we have to allow multiple channels per connection somehow..
        return dbHandler.getChannel(channelHashToClose);
    }

    @Override
    public void onInboundMessage (Message message) {
        consumeMessage(message);
    }

    @Override
    public boolean consumesInboundMessage (Object object) {
        //This processor is placed in front of the payment processor
        //Once blocked, we will not allow any message past this layer
        return object instanceof LNClose || isBlocked;
    }

    @Override
    public boolean consumesOutboundMessage (Object object) {
        //This processor is placed in front of the payment processor
        //Once blocked, we will not allow any message past this layer
        return isBlocked;
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        //TODO move obligation to save channel object in ChannelManagement
        //Need to also keep track of channels after reconnecting..

        setNode(new NodeKey(node.pubKeyClient));
        this.messageExecutor = messageExecutor;
        this.channelManager.addChannelCloser(getNode(), this);
        this.messageExecutor.sendNextLayerActive();
    }

    private void consumeMessage (Message message) {
        if (message instanceof LNCloseAMessage) {
            processChannelClose((LNCloseAMessage) message);
        }
    }

    private void processChannelClose (LNCloseAMessage message) {
        checkClosingMessage(message);
        Transaction transaction = getClosingTransaction(channel.channelStatus, message.feePerByte);
        List<TransactionSignature> signatures = getTransactionSignatures(transaction);

        isBlocked = true;
        Channel channel = getChannel();
        channel.isReady = false;
        channel.closingSignatures = message.getSignatureList();
        dbHandler.updateChannel(channel);

        if (channel.phase != CLOSE_REQUESTED_SERVER) {
            channel.phase = CLOSE_REQUESTED_CLIENT;
            sendCloseMessage(signatures);
            //Okay, so the other party sent us correct signatures to close down the channel..
        }
        Script inputScript = ScriptTools.getCommitInputScript(
                message.getSignatureList().get(0).encodeToBitcoin(),
                signatures.get(0).encodeToBitcoin(),
                channel.keyClient,
                channel.keyServer);

        transaction.getInput(0).setScriptSig(inputScript);

        blockchainHelper.broadcastTransaction(transaction);
        onChannelClose();
    }

    private void checkClosingMessage (LNCloseAMessage message) {
        if (!checkFee(message)) {
            //TODO return some error message
            //TODO handle the error message appropriately..
            throw new LNCloseException("Fee not within allowed boundaries..");
        }
        if (channelHashToClose == null) {
            channelHashToClose = Sha256Hash.wrap(message.channelHash);
        }
        this.channel = getChannel();
        //TODO fix working out the correct reverse strategy for channels that still have payments included
        //ChannelStatus status = getChannelStatus(channel.channelStatus.getCloneReversed()).getCloneReversed();
        ChannelStatus status = channel.channelStatus;

        Transaction transaction = getClosingTransaction(status, message.feePerByte);

        List<TransactionSignature> signatureList = message.getSignatureList();

        int i = 0;
        for (TransactionSignature signature : signatureList) {

            boolean correct = Tools.checkSignature(transaction, i, ScriptTools.getAnchorOutputScript(channel.keyClient, channel.keyServer), channel.keyClient,
                    signature);
            if (!correct) {
                throw new LNCloseException("Signature is not correct..");
            }
            i++;
        }
    }

    private void onChannelClose () {
        eventHelper.onChannelClosed(channel);
        callback.execute(new SuccessResult());
        channelManager.onChannelClosed(channel);
        messageExecutor.closeConnection();
    }

    @Override
    public void onLayerClose () {
        channelManager.removeChannelCloser(getNode());
        scheduler.shutdown();
    }

}
