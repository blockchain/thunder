package network.thunder.core.communication.processor.implementations.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.database.objects.Channel;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementPayment implements QueueElement {
    @Override
    public ChannelStatus produceNewChannelStatus (Channel channel) {
        return null;
    }
}
