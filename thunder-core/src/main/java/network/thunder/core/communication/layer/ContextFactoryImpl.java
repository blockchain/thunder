package network.thunder.core.communication.layer;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.channel.ChannelManagerImpl;
import network.thunder.core.communication.layer.high.channel.establish.LNEstablishProcessor;
import network.thunder.core.communication.layer.high.channel.establish.messages.LNEstablishMessageFactory;
import network.thunder.core.communication.layer.high.channel.establish.messages.LNEstablishMessageFactoryImpl;
import network.thunder.core.communication.layer.high.payments.*;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentMessageFactory;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentMessageFactoryImpl;
import network.thunder.core.communication.layer.low.authentication.AuthenticationProcessor;
import network.thunder.core.communication.layer.low.authentication.messages.AuthenticationMessageFactory;
import network.thunder.core.communication.layer.low.authentication.messages.AuthenticationMessageFactoryImpl;
import network.thunder.core.communication.layer.low.encryption.EncryptionProcessor;
import network.thunder.core.communication.layer.low.encryption.MessageEncrypter;
import network.thunder.core.communication.layer.low.encryption.MessageEncrypterImpl;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptionMessageFactory;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptionMessageFactoryImpl;
import network.thunder.core.communication.layer.low.serialisation.MessageSerializer;
import network.thunder.core.communication.layer.low.serialisation.MessageSerializerImpl;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.*;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.messages.GossipMessageFactory;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.messages.GossipMessageFactoryImpl;
import network.thunder.core.communication.layer.middle.broadcasting.sync.SyncProcessor;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.SyncMessageFactory;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.SyncMessageFactoryImpl;
import network.thunder.core.communication.layer.middle.peerseed.messages.PeerSeedMessageFactory;
import network.thunder.core.communication.layer.middle.peerseed.PeerSeedProcessor;
import network.thunder.core.communication.layer.middle.peerseed.messages.PeerSeedMessageFactoryImpl;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.blockchain.MockBlockchainHelper;
import network.thunder.core.communication.layer.low.authentication.AuthenticationProcessorImpl;
import network.thunder.core.communication.layer.low.encryption.EncryptionProcessorImpl;
import network.thunder.core.communication.layer.high.channel.establish.LNEstablishProcessorImpl;
import network.thunder.core.communication.layer.middle.peerseed.PeerSeedProcessorImpl;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogicImpl;
import network.thunder.core.communication.layer.high.payments.LNPaymentProcessorImpl;
import network.thunder.core.communication.layer.middle.broadcasting.sync.SyncProcessorImpl;
import network.thunder.core.communication.layer.middle.broadcasting.sync.SynchronizationHelper;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.high.payments.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.wallet.WalletHelper;
import network.thunder.core.helper.wallet.WalletHelperImpl;
import network.thunder.core.communication.ServerObject;
import org.bitcoinj.core.Wallet;

/**
 * Created by matsjerratsch on 18/01/2016.
 */
public class ContextFactoryImpl implements ContextFactory {
    DBHandler dbHandler;
    LNEventHelper eventHelper;

    SynchronizationHelper syncHelper;
    GossipSubject gossipSubject;
    BroadcastHelper broadcastHelper;
    WalletHelper walletHelper;

    LNPaymentHelper paymentHelper;

    LNOnionHelper onionHelper = new LNOnionHelperImpl();

    ServerObject serverObject;

    BlockchainHelper blockchainHelper;
    ChannelManager channelManager;

    public ContextFactoryImpl (ServerObject node, DBHandler dbHandler, Wallet wallet, LNEventHelper eventHelper) {
        this.serverObject = node;
        this.dbHandler = dbHandler;
        this.eventHelper = eventHelper;
        this.walletHelper = new WalletHelperImpl(wallet);

        GossipSubjectImpl gossipSubject = new GossipSubjectImpl(dbHandler, eventHelper);
        this.gossipSubject = gossipSubject;
        this.broadcastHelper = gossipSubject;

        this.syncHelper = new SynchronizationHelper(dbHandler);

        this.paymentHelper = new LNPaymentHelperImpl(this, dbHandler);
        this.blockchainHelper = new MockBlockchainHelper();
        this.channelManager = new ChannelManagerImpl(getBlockchainHelper());
    }

    @Override
    public MessageSerializer getMessageSerializer () {
        return new MessageSerializerImpl();
    }

    @Override
    public MessageEncrypter getMessageEncrypter () {
        return new MessageEncrypterImpl(getMessageSerializer());
    }

    @Override
    public EncryptionProcessor getEncryptionProcessor (ClientObject node) {
        return new EncryptionProcessorImpl(this, node);
    }

    @Override
    public AuthenticationProcessor getAuthenticationProcessor (ClientObject node) {
        return new AuthenticationProcessorImpl(this, node);
    }

    @Override
    public PeerSeedProcessor getPeerSeedProcessor (ClientObject node) {
        return new PeerSeedProcessorImpl(this, dbHandler, node);
    }

    @Override
    public SyncProcessor getSyncProcessor (ClientObject node) {
        return new SyncProcessorImpl(this, node);
    }

    @Override
    public GossipProcessor getGossipProcessor (ClientObject node) {
        return new GossipProcessorImpl(this, dbHandler, node);
    }

    @Override
    public LNEstablishProcessor getLNEstablishProcessor (ClientObject node) {
        return new LNEstablishProcessorImpl(this, dbHandler, node);
    }

    @Override
    public LNPaymentProcessor getLNPaymentProcessor (ClientObject node) {
        return new LNPaymentProcessorImpl(this, dbHandler, node);
    }

    @Override
    public LNPaymentHelper getPaymentHelper () {
        return paymentHelper;
    }

    @Override
    public LNOnionHelper getOnionHelper () {
        return onionHelper;
    }

    @Override
    public LNEventHelper getEventHelper () {
        return eventHelper;
    }

    @Override
    public BroadcastHelper getBroadcastHelper () {
        return broadcastHelper;
    }

    @Override
    public SynchronizationHelper getSyncHelper () {
        return syncHelper;
    }

    @Override
    public WalletHelper getWalletHelper () {
        return walletHelper;
    }

    @Override
    public GossipSubject getGossipSubject () {
        return gossipSubject;
    }

    @Override
    public LNPaymentLogic getLNPaymentLogic () {
        return new LNPaymentLogicImpl(getLNPaymentMessageFactory(), dbHandler);
    }

    @Override
    public ServerObject getServerSettings () {
        return serverObject;
    }

    @Override
    public EncryptionMessageFactory getEncryptionMessageFactory () {
        return new EncryptionMessageFactoryImpl();
    }

    @Override
    public AuthenticationMessageFactory getAuthenticationMessageFactory () {
        return new AuthenticationMessageFactoryImpl();
    }

    @Override
    public PeerSeedMessageFactory getPeerSeedMessageFactory () {
        return new PeerSeedMessageFactoryImpl();
    }

    @Override
    public SyncMessageFactory getSyncMessageFactory () {
        return new SyncMessageFactoryImpl();
    }

    @Override
    public GossipMessageFactory getGossipMessageFactory () {
        return new GossipMessageFactoryImpl();
    }

    @Override
    public LNEstablishMessageFactory getLNEstablishMessageFactory () {
        return new LNEstablishMessageFactoryImpl();
    }

    @Override
    public LNPaymentMessageFactory getLNPaymentMessageFactory () {
        return new LNPaymentMessageFactoryImpl(dbHandler);
    }

    @Override
    public LNRoutingHelper getLNRoutingHelper () {
        return new LNRoutingHelperImpl(dbHandler);
    }

    @Override
    public BlockchainHelper getBlockchainHelper () {
        return blockchainHelper;
    }

    @Override
    public ChannelManager getChannelManager () {
        return channelManager;
    }

}
