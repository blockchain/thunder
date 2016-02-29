package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventListener;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.Payment;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.mesh.NodeClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 08/02/2016.
 */
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
    public void onConnectionOpened (NodeClient node) {
        for (LNEventListener listener : listeners) {
            listener.onConnectionOpened(node);
        }
    }

    @Override
    public void onConnectionClosed (NodeClient node) {
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
    public void onPaymentRefunded (Payment payment) {
        for (LNEventListener listener : listeners) {
            listener.onPaymentRefunded(payment);
        }
    }

    @Override
    public void onPaymentExchangeDone () {

    }

    @Override
    public void onPaymentCompleted (PaymentSecret payment) {
        for (LNEventListener listener : listeners) {
            listener.onPaymentCompleted(payment);
        }
    }

    @Override
    public void onP2PDataReceived () {
        for (LNEventListener listener : listeners) {
            listener.onP2PDataReceived();
        }
    }
}
