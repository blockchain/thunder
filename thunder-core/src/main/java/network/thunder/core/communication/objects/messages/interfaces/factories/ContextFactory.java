package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.interfaces.helper.*;
import network.thunder.core.communication.processor.implementations.gossip.BroadcastHelper;
import network.thunder.core.communication.processor.implementations.gossip.GossipSubject;
import network.thunder.core.communication.processor.implementations.sync.SynchronizationHelper;
import network.thunder.core.communication.processor.interfaces.*;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;

/**
 * Created by matsjerratsch on 18/01/2016.
 */
public interface ContextFactory {
    MessageSerializer getMessageSerializer ();

    MessageEncrypter getMessageEncrypter ();

    EncryptionProcessor getEncryptionProcessor (NodeClient node);

    AuthenticationProcessor getAuthenticationProcessor (NodeClient node);

    PeerSeedProcessor getPeerSeedProcessor (NodeClient node);

    SyncProcessor getSyncProcessor (NodeClient node);

    GossipProcessor getGossipProcessor (NodeClient node);

    LNEstablishProcessor getLNEstablishProcessor (NodeClient node);

    LNPaymentProcessor getLNPaymentProcessor (NodeClient node);

    LNPaymentHelper getPaymentHelper ();

    LNOnionHelper getOnionHelper ();

    LNEventHelper getEventHelper ();

    BroadcastHelper getBroadcastHelper ();

    SynchronizationHelper getSyncHelper ();

    WalletHelper getWalletHelper ();

    GossipSubject getGossipSubject ();

    LNPaymentLogic getLNPaymentLogic ();

    NodeServer getServerSettings ();

    EncryptionMessageFactory getEncryptionMessageFactory ();

    AuthenticationMessageFactory getAuthenticationMessageFactory ();

    PeerSeedMessageFactory getPeerSeedMessageFactory ();

    SyncMessageFactory getSyncMessageFactory ();

    GossipMessageFactory getGossipMessageFactory ();

    LNEstablishMessageFactory getLNEstablishMessageFactory ();

    LNPaymentMessageFactory getLNPaymentMessageFactory ();

    LNRoutingHelper getLNRoutingHelper ();

    BlockchainHelper getBlockchainHelper ();

    ChannelManager getChannelManager ();
}
