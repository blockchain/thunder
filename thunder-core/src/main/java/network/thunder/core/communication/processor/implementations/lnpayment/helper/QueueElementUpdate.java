package network.thunder.core.communication.processor.implementations.lnpayment.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementUpdate extends QueueElement {

    @Override
    public ChannelStatus produceNewChannelStatus (ChannelStatus channel) {
        return channel;
    }
}
