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
    public MessageSerializer getMessageSerializer ();

    public MessageEncrypter getMessageEncrypter ();

    public EncryptionProcessor getEncryptionProcessor (Node node);

    public AuthenticationProcessor getAuthenticationProcessor (Node node);

    public SyncProcessor getSyncProcessor (Node node);

    public GossipProcessor getGossipProcessor (Node node);

    public LNEstablishProcessor getLNEstablishProcessor (Node node);

    public LNPaymentProcessor getLNPaymentProcessor (Node node);
}
