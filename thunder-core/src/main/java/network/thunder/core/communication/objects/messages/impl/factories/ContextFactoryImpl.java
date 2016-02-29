package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.*;
import network.thunder.core.communication.objects.messages.impl.routing.LNRoutingHelperImpl;
import network.thunder.core.communication.objects.messages.interfaces.factories.*;
import network.thunder.core.communication.objects.messages.interfaces.helper.*;
import network.thunder.core.communication.processor.implementations.AuthenticationProcessorImpl;
import network.thunder.core.communication.processor.implementations.EncryptionProcessorImpl;
import network.thunder.core.communication.processor.implementations.LNEstablishProcessorImpl;
import network.thunder.core.communication.processor.implementations.PeerSeedProcessorImpl;
import network.thunder.core.communication.processor.implementations.gossip.BroadcastHelper;
import network.thunder.core.communication.processor.implementations.gossip.GossipProcessorImpl;
import network.thunder.core.communication.processor.implementations.gossip.GossipSubject;
import network.thunder.core.communication.processor.implementations.gossip.GossipSubjectImpl;
import network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentLogicImpl;
import network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentProcessorImpl;
import network.thunder.core.communication.processor.implementations.sync.SyncProcessorImpl;
import network.thunder.core.communication.processor.implementations.sync.SynchronizationHelper;
import network.thunder.core.communication.processor.interfaces.*;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
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

    NodeServer nodeServer;

    public ContextFactoryImpl (NodeServer node, DBHandler dbHandler, Wallet wallet, LNEventHelper eventHelper) {
        this.nodeServer = node;
        this.dbHandler = dbHandler;
        this.eventHelper = eventHelper;
        this.walletHelper = new WalletHelperImpl(wallet);

        GossipSubjectImpl gossipSubject = new GossipSubjectImpl(dbHandler, eventHelper);
        this.gossipSubject = gossipSubject;
        this.broadcastHelper = gossipSubject;

        this.syncHelper = new SynchronizationHelper(dbHandler);

        this.paymentHelper = new LNPaymentHelperImpl(this, dbHandler);
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
    public EncryptionProcessor getEncryptionProcessor (NodeClient node) {
        return new EncryptionProcessorImpl(this, node);
    }

    @Override
    public AuthenticationProcessor getAuthenticationProcessor (NodeClient node) {
        return new AuthenticationProcessorImpl(this, node);
    }

    @Override
    public PeerSeedProcessor getPeerSeedProcessor (NodeClient node) {
        return new PeerSeedProcessorImpl(this, dbHandler, node);
    }

    @Override
    public SyncProcessor getSyncProcessor (NodeClient node) {
        return new SyncProcessorImpl(this, node);
    }

    @Override
    public GossipProcessor getGossipProcessor (NodeClient node) {
        return new GossipProcessorImpl(this, dbHandler, node);
    }

    @Override
    public LNEstablishProcessor getLNEstablishProcessor (NodeClient node) {
        return new LNEstablishProcessorImpl(this, dbHandler, node);
    }

    @Override
    public LNPaymentProcessor getLNPaymentProcessor (NodeClient node) {
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
        return new LNPaymentLogicImpl(dbHandler);
    }

    @Override
    public NodeServer getServerSettings () {
        return nodeServer;
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

}
