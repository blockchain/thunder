package network.thunder.core.communication.processor.implementations.lnpayment.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public abstract class QueueElement {
    long addedTimestamp;

    public QueueElement () {
        addedTimestamp = System.currentTimeMillis();
    }

    abstract public ChannelStatus produceNewChannelStatus (ChannelStatus channel, LNPaymentHelper paymentHelper);
}