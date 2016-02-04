package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.interfaces.helper.MessageEncrypter;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializer;
import network.thunder.core.communication.processor.interfaces.*;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 18/01/2016.
 */
public interface ContextFactory {
    MessageSerializer getMessageSerializer ();

    MessageEncrypter getMessageEncrypter ();

    EncryptionProcessor getEncryptionProcessor (Node node);

    AuthenticationProcessor getAuthenticationProcessor (Node node);

    PeerSeedProcessor getPeerSeedProcessor (Node node);

    SyncProcessor getSyncProcessor (Node node);

    GossipProcessor getGossipProcessor (Node node);

    LNEstablishProcessor getLNEstablishProcessor (Node node);

    LNPaymentProcessor getLNPaymentProcessor (Node node);
}
