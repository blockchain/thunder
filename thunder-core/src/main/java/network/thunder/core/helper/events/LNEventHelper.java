package network.thunder.core.helper.events;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentWrapper;

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

    void onPaymentRefunded (PaymentData payment);

    void onPaymentRedeemed (PaymentData payment);

    void onPaymentAdded (NodeKey nodeKey, PaymentData payment);

    void onPaymentExchangeDone ();

    void onP2PDataReceived ();

}
