package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.Payment;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.mesh.NodeClient;

/**
 * Created by matsjerratsch on 08/02/2016.
 */
public abstract class LNEventListener {
    public void onConnectionOpened (NodeClient node) {
        onEvent();
    }

    public void onConnectionClosed (NodeClient node) {
        onEvent();
    }

    public void onChannelOpened (Channel channel) {
        onEvent();
    }

    public void onChannelClosed (Channel channel) {
        onEvent();
    }

    public void onReceivedIP (PubkeyIPObject ip) {
        onEvent();
    }

    public void onPaymentRelayed (PaymentWrapper wrapper) {
        onEvent();
    }

    public void onPaymentRefunded (Payment payment) {
        onEvent();
    }

    public void onPaymentCompleted (PaymentSecret payment) {
        onEvent();
    }

    public void onPaymentExchangeDone () {
        onEvent();
    }

    public void onP2PDataReceived () {
        onEvent();
    }

    public void onEvent () {

    }
}
