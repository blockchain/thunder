package network.thunder.core.communication.processor.implementations;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.ChannelStatusObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyChannelObject;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNEstablishMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.BlockchainHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.WalletHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.OnBlockCommand;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.OnTxCommand;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.LNEstablish;
import network.thunder.core.communication.processor.ConnectionResult;
import network.thunder.core.communication.processor.exceptions.LNEstablishException;
import network.thunder.core.communication.processor.implementations.gossip.BroadcastHelper;
import network.thunder.core.communication.processor.interfaces.LNEstablishProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishProcessorImpl extends LNEstablishProcessor {
    public static final double PERCENTAGE_OF_FUNDS_PER_CHANNEL = 0.1;
    public static final int MIN_CONFIRMATIONS = 0;

    WalletHelper walletHelper;
    LNEstablishMessageFactory messageFactory;
    BroadcastHelper broadcastHelper;
    LNEventHelper eventHelper;
    DBHandler dbHandler;
    NodeClient node;
    NodeServer nodeServer;
    BlockchainHelper blockchainHelper;

    MessageExecutor messageExecutor;

    public Channel channel;
    int status = 0;

    public LNEstablishProcessorImpl (ContextFactory contextFactory, DBHandler dbHandler, NodeClient node) {
        this.walletHelper = contextFactory.getWalletHelper();
        this.messageFactory = contextFactory.getLNEstablishMessageFactory();
        this.broadcastHelper = contextFactory.getBroadcastHelper();
        this.eventHelper = contextFactory.getEventHelper();
        this.dbHandler = dbHandler;
        this.node = node;
        this.nodeServer = contextFactory.getServerSettings();
        this.blockchainHelper = contextFactory.getBlockchainHelper();
    }

    @Override
    public void onInboundMessage (Message message) {
        try {
            consumeMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            messageExecutor.sendMessageUpwards(messageFactory.getFailureMessage(e.getMessage()));
            node.result = ConnectionResult.ERROR;
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
        //TODO check for existing channels, check if we are still waiting for them to gather enough confirmations, ...
        this.messageExecutor = messageExecutor;
        if (!node.isServer) {
            sendEstablishMessageA();
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

    private void processMessageA (Message message) {
        checkStatus(0);
        LNEstablishAMessage m = (LNEstablishAMessage) message;
        prepareNewChannel();
        m.saveToChannel(channel);
        sendEstablishMessageB();
    }

    private void processMessageB (Message message) {
        checkStatus(2);
        LNEstablishBMessage m = (LNEstablishBMessage) message;
        m.saveToChannel(channel);
        sendEstablishMessageC();
    }

    private void processMessageC (Message message) {
        checkStatus(3);
        LNEstablishCMessage m = (LNEstablishCMessage) message;
        m.saveToChannel(channel);
        channel.verifyEscapeSignatures();
        sendEstablishMessageD();
        onChannelEstablished();
    }

    private void processMessageD (Message message) {
        checkStatus(4);
        LNEstablishDMessage m = (LNEstablishDMessage) message;
        m.saveToChannel(channel);
        channel.verifyEscapeSignatures();
        onChannelEstablished();
    }

    private void onChannelEstablished () {
        //TODO: Everything needed has been exchanged. We can now open the channel / wait to see the other channel on the blockchain.
        //          We need a WatcherClass on the BlockChain for that, to wait till the anchors are sufficiently deep in the blockchain.
        blockchainHelper.broadcastTransaction(channel.getAnchorTransactionServer());

        blockchainHelper.addTxListener(new OnTxCommand() {
            @Override
            public boolean compare (Transaction tx) {
                return channel.getAnchorTxHashClient().equals(tx.getHash());
            }

            @Override
            public void execute (Transaction tx) {
                if (MIN_CONFIRMATIONS > 0) {
                    blockchainHelper.addBlockListener(new OnBlockCommand() {
                        boolean found = false;
                        int confirmations = 0;

                        @Override
                        public boolean execute (Block block) {
                            if (!found) {
                                if (block.getTransactions().contains(tx)) {
                                    found = true;
                                }
                            }

                            if (found) {
                                confirmations++;
                            }

                            if (confirmations >= MIN_CONFIRMATIONS) {
                                onEnoughConfirmations();
                                return true;
                            }

                            return false;
                        }
                    });
                } else {
                    onEnoughConfirmations();
                }
            }
        });
    }

    private void onEnoughConfirmations () {
        System.out.println("AAA");
        channel.initiateChannelStatus(nodeServer.configuration);
        dbHandler.saveChannel(channel);
        broadcastChannelObject();
        eventHelper.onChannelOpened(channel);
        messageExecutor.sendNextLayerActive();
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
        channel = new Channel(node.pubKeyClient.getPubKey(), nodeServer.pubKeyServer, getAmountForNewChannel());
        status = 1;
    }

    private void broadcastChannelObject () {
        PubkeyChannelObject channelObject = PubkeyChannelObject.getRandomObject();
        ChannelStatusObject statusObject = new ChannelStatusObject();
        statusObject.pubkeyA = nodeServer.pubKeyServer.getPubKey();
        statusObject.pubkeyB = node.pubKeyClient.getPubKey();
        broadcastHelper.broadcastNewObject(channelObject);
        broadcastHelper.broadcastNewObject(statusObject);
    }

    private long getAmountForNewChannel () {
        return (long) (walletHelper.getSpendableAmount() * PERCENTAGE_OF_FUNDS_PER_CHANNEL);
    }

    private void checkStatus (int expected) {
        if (status != expected) {
            throw new LNEstablishException("Status not correct.. Is: " + status + " Expected: " + expected);
        }
    }

}
