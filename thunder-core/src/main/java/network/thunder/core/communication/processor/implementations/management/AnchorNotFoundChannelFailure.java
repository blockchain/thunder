package network.thunder.core.communication.processor.implementations.management;

import network.thunder.core.communication.objects.messages.interfaces.helper.BlockchainHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.ChannelManager;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ChannelFailureAction;
import network.thunder.core.database.objects.Channel;

public class AnchorNotFoundChannelFailure implements ChannelFailureAction {

    Channel channel;

    public AnchorNotFoundChannelFailure (Channel channel) {
        this.channel = channel;
    }

    @Override
    public void execute (ChannelManager manager, BlockchainHelper blockchainHelper) {
        //TODO implement logic to claim funds
    }
}
