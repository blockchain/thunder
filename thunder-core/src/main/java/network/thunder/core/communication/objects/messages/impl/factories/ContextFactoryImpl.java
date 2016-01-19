package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.MessageEncrypterImpl;
import network.thunder.core.communication.objects.messages.impl.MessageSerializerImpl;
import network.thunder.core.communication.objects.messages.interfaces.factories.*;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageEncrypter;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializer;
import network.thunder.core.communication.objects.messages.interfaces.helper.WalletHelper;
import network.thunder.core.communication.processor.implementations.AuthenticationProcessorImpl;
import network.thunder.core.communication.processor.implementations.EncryptionProcessorImpl;
import network.thunder.core.communication.processor.implementations.LNEstablishProcessorImpl;
import network.thunder.core.communication.processor.implementations.gossip.GossipProcessorImpl;
import network.thunder.core.communication.processor.implementations.gossip.GossipSubject;
import network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentLogicImpl;
import network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentProcessorImpl;
import network.thunder.core.communication.processor.implementations.sync.SyncProcessorImpl;
import network.thunder.core.communication.processor.implementations.sync.SynchronizationHelper;
import network.thunder.core.communication.processor.interfaces.*;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 18/01/2016.
 */
public class ContextFactoryImpl implements ContextFactory {
    DBHandler dbHandler;
    SynchronizationHelper syncHelper;
    GossipSubject gossipSubject;
    WalletHelper walletHelper;

    @Override
    public MessageSerializer getMessageSerializer () {
        return new MessageSerializerImpl();
    }

    @Override
    public MessageEncrypter getMessageEncrypter () {
        return new MessageEncrypterImpl(getMessageSerializer());
    }

    @Override
    public EncryptionProcessor getEncryptionProcessor (Node node) {
        MessageEncrypter messageEncrypter = getMessageEncrypter();
        EncryptionMessageFactory encryptionMessageFactory = new EncryptionMessageFactoryImpl();
        return new EncryptionProcessorImpl(encryptionMessageFactory, messageEncrypter, node);
    }

    @Override
    public AuthenticationProcessor getAuthenticationProcessor (Node node) {
        return new AuthenticationProcessorImpl(new AuthenticationMessageFactoryImpl(), node);
    }

    @Override
    public SyncProcessor getSyncProcessor (Node node) {
        SyncMessageFactory messageFactory = new SyncMessageFactoryImpl();
        return new SyncProcessorImpl(messageFactory, node, syncHelper);
    }

    @Override
    public GossipProcessor getGossipProcessor (Node node) {
        GossipMessageFactory messageFactory = new GossipMessageFactoryImpl();
        return new GossipProcessorImpl(messageFactory, gossipSubject, dbHandler, node);
    }

    @Override
    public LNEstablishProcessor getLNEstablishProcessor (Node node) {
        LNEstablishFactory messageFactory = new LNEstablishMessageFactoryImpl();
        return new LNEstablishProcessorImpl(walletHelper, messageFactory, node);
    }

    @Override
    public LNPaymentProcessor getLNPaymentProcessor (Node node) {
        LNPaymentMessageFactory messageFactory = new LNPaymentMessageFactoryImpl(dbHandler);
        LNPaymentLogic paymentLogic = new LNPaymentLogicImpl(dbHandler);
        return new LNPaymentProcessorImpl(messageFactory, paymentLogic, dbHandler, node);
    }

}
