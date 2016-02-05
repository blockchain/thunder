package network.thunder.core.communication.processor.implementations.lnpayment.helper;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class QueueElementUpdate extends QueueElement {

    @Override
    public ChannelStatus produceNewChannelStatus (ChannelStatus channel, LNPaymentHelper paymentHelper) {
        return channel;
    }
}
