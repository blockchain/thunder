package network.thunder.core.communication.layer.high.payments.queue;

import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;

public class QueueElementUpdate extends QueueElement {

    @Override
    public ChannelUpdate produceNewChannelStatus (ChannelStatus channel, ChannelUpdate channelUpdate, LNPaymentHelper paymentHelper) {
        return channelUpdate;
    }
}
