package network.thunder.core.helper.events;

import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.database.objects.Payment;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.communication.ClientObject;

public interface LNEventHelper {

    void addListener (LNEventListener listener);

    void removeListener (LNEventListener listener);

    //Events..
    void onConnectionOpened (ClientObject node);

    void onConnectionClosed (ClientObject node);

    void onChannelOpened (Channel channel);

    void onChannelClosed (Channel channel);

    void onReceivedIP (PubkeyIPObject ip);

    void onPaymentRelayed (PaymentWrapper wrapper);

    void onPaymentRefunded (Payment payment);

    void onPaymentExchangeDone ();

    void onPaymentCompleted (PaymentSecret payment);

    void onP2PDataReceived ();

}
