package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import org.bitcoinj.core.ECKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LNOnionHelper {
    PeeledOnion loadMessage (ECKey keyServer, OnionObject encryptedOnionObject);

    //Old way of creating onion objects just out of a raw key list without any meta data
    OnionObject createOnionObject (List<byte[]> keyList, byte[] payload);

    /**
     * Fill OnionObject with meta data about the route that will make it more difficult to probe the final receiver.
     * Use to create completely source-driven routing
     *
     * @param amount        Amount for the first hop, will use ChannelStatusObject to calculate all amounts down the route
     * @param paymentSecret Can be null to not include a payment secret into the onion object
     */
    OnionObject createOnionObject (List<ChannelStatusObject> route,
                                   NodeKey finalReceiver,
                                   int timeout,
                                   long amount,
                                   @Nullable PaymentSecret paymentSecret);
    /**
     * Create OnionObject with the Rendezvous-Point object we got from the receiver. If the receiver also sent us an ephemeral key, we can transmit
     * the payment secret and the final amount the receiver should see
     */
    OnionObject createOnionObject (List<ChannelStatusObject> route,
                                   NodeKey rpNode,
                                   OnionObject rpObject,
                                   long amount,
                                   @Nullable PaymentSecret paymentSecret,
                                   @Nullable ECKey ephemeralReceiver);

    OnionObject createRPObject (List<ChannelStatusObject> route, NodeKey keyServer);

}
