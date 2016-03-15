package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ChannelFailureAction;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.Command;
import network.thunder.core.database.objects.Channel;

public interface ChannelManager {
    void onExchangeDone (Channel channel, Command successCommand);

    void onAnchorDone (Channel channel);

    void onAnchorFailure (Channel channel, ChannelFailureAction failureAction);

    boolean queryChannelReady (Channel channel);

}
