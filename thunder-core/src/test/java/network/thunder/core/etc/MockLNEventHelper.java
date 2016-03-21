package network.thunder.core.etc;

import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.events.LNEventListener;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.database.objects.Payment;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.communication.ClientObject;

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
