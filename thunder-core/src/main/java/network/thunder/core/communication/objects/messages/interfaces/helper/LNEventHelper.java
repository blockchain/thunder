package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.Payment;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.mesh.Node;

public interface LNEventHelper {

    void addListener (LNEventListener listener);

    void removeListener (LNEventListener listener);

    //Events..
    void onConnectionOpened (Node node);

    void onConnectionClosed (Node node);

    void onChannelOpened (Channel channel);

    void onChannelClosed (Channel channel);

    void onReceivedIP (PubkeyIPObject ip);

    void onPaymentRelayed (PaymentWrapper wrapper);

    void onPaymentRefunded (Payment payment);

}
