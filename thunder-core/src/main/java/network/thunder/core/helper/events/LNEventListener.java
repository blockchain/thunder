package network.thunder.core.helper.events;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentWrapper;

public abstract class LNEventListener {
    public void onConnectionOpened (ClientObject node) {
        onEvent();
    }

    public void onConnectionClosed (ClientObject node) {
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

    public void onPaymentRefunded (PaymentData payment) {
        onEvent();
    }

    public void onPaymentCompleted (PaymentData payment) {
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
