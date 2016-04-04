package network.thunder.core.communication.layer.high.payments.queue;

import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;

public abstract class QueueElement {
    long addedTimestamp;

    public QueueElement () {
        addedTimestamp = System.currentTimeMillis();
    }

    abstract public ChannelUpdate produceNewChannelStatus (ChannelStatus channel, ChannelUpdate channelUpdate, LNPaymentHelper paymentHelper);
}