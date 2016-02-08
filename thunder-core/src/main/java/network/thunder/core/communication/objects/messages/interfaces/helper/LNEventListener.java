package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.Payment;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 08/02/2016.
 */
public abstract class LNEventListener {
    public void onConnectionOpened (Node node) {

    }

    public void onConnectionClosed (Node node) {

    }

    public void onChannelOpened (Channel channel) {

    }

    public void onChannelClosed (Channel channel) {

    }

    public void onReceivedIP (PubkeyIPObject ip) {

    }

    public void onPaymentRelayed (PaymentWrapper wrapper) {

    }

    public void onPaymentRefunded (Payment payment) {

    }
}
