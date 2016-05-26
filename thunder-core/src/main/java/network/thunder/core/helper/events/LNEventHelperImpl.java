package network.thunder.core.helper.events;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentWrapper;

import java.util.ArrayList;
import java.util.List;

public class LNEventHelperImpl implements LNEventHelper {
    List<LNEventListener> listeners = new ArrayList<>();

    @Override
    public void addListener (LNEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener (LNEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onConnectionOpened (ClientObject node) {
        for (LNEventListener listener : listeners) {
            listener.onConnectionOpened(node);
        }
    }

    @Override
    public void onConnectionClosed (ClientObject node) {
        for (LNEventListener listener : listeners) {
            listener.onConnectionClosed(node);
        }
    }

    @Override
    public void onChannelOpened (Channel channel) {
        for (LNEventListener listener : listeners) {
            listener.onChannelOpened(channel);
        }
    }

    @Override
    public void onChannelClosed (Channel channel) {
        for (LNEventListener listener : listeners) {
            listener.onChannelClosed(channel);
        }
    }

    @Override
    public void onReceivedIP (PubkeyIPObject ip) {
        for (LNEventListener listener : listeners) {
            listener.onReceivedIP(ip);
        }
    }

    @Override
    public void onPaymentRelayed (PaymentWrapper wrapper) {
        for (LNEventListener listener : listeners) {
            listener.onPaymentRelayed(wrapper);
        }
    }

    @Override
    public void onPaymentRefunded (PaymentData payment) {
        for (LNEventListener listener : listeners) {
            listener.onPaymentRefunded(payment);
        }
    }

    @Override
    public void onPaymentExchangeDone () {
        for (LNEventListener listener : listeners) {
            listener.onPaymentExchangeDone();
        }
    }

    @Override
    public void onPaymentRedeemed (PaymentData payment) {
        for (LNEventListener listener : listeners) {
            listener.onPaymentCompleted(payment);
        }
    }

    @Override
    public void onPaymentAdded (NodeKey nodeKey, PaymentData payment) {
        for (LNEventListener listener : listeners) {
            listener.onPaymentAdded(nodeKey, payment);
        }
    }

    @Override
    public void onP2PDataReceived () {
        for (LNEventListener listener : listeners) {
            listener.onP2PDataReceived();
        }
    }
}
