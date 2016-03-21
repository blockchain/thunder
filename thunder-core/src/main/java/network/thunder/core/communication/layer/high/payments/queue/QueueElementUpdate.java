package network.thunder.core.communication.layer.high.payments.queue;

import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementUpdate extends QueueElement {

    @Override
    public ChannelStatus produceNewChannelStatus (ChannelStatus channel, LNPaymentHelper paymentHelper) {
        return channel;
    }
}
