package network.thunder.core.etc;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventListener;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.Payment;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.mesh.NodeClient;

/**
 * Created by matsjerratsch on 08/02/2016.
 */
public class MockLNEventHelper implements LNEventHelper {
    @Override
    public void addListener (LNEventListener listener) {

    }

    @Override
    public void removeListener (LNEventListener listener) {

    }

    @Override
    public void onConnectionOpened (NodeClient node) {

    }

    @Override
    public void onConnectionClosed (NodeClient node) {

    }

    @Override
    public void onChannelOpened (Channel channel) {

    }

    @Override
    public void onChannelClosed (Channel channel) {

    }

    @Override
    public void onReceivedIP (PubkeyIPObject ip) {

    }

    @Override
    public void onPaymentRelayed (PaymentWrapper wrapper) {

    }

    @Override
    public void onPaymentRefunded (Payment payment) {

    }

    @Override
    public void onPaymentExchangeDone () {

    }

    @Override
    public void onPaymentCompleted (PaymentSecret payment) {

    }

    @Override
    public void onP2PDataReceived () {

    }
}
