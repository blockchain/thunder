package network.thunder.core.etc;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventListener;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.Payment;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.mesh.Node;

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
    public void onConnectionOpened (Node node) {

    }

    @Override
    public void onConnectionClosed (Node node) {

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
}
