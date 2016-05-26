package network.thunder.core.etc;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.events.LNEventListener;

public class MockLNEventHelper implements LNEventHelper {
    @Override
    public void addListener (LNEventListener listener) {

    }

    @Override
    public void removeListener (LNEventListener listener) {

    }

    @Override
    public void onConnectionOpened (ClientObject node) {

    }

    @Override
    public void onConnectionClosed (ClientObject node) {

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
    public void onPaymentRefunded (PaymentData payment) {

    }

    @Override
    public void onPaymentRedeemed (PaymentData payment) {

    }

    @Override
    public void onPaymentAdded (NodeKey nodeKey, PaymentData payment) {

    }

    @Override
    public void onPaymentExchangeDone () {

    }

    @Override
    public void onP2PDataReceived () {

    }
}
