package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 13/01/2016.
 */
public class LNPaymentDBHandlerMock extends DBHandlerMock {

    @Override
    public Channel getChannel (Node node) {
        Channel channel = new Channel();
        channel.channelStatus = new ChannelStatus();
        return channel;
    }
}
