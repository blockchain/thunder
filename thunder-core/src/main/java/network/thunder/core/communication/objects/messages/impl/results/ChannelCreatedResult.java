package network.thunder.core.communication.objects.messages.impl.results;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ConnectionResult;
import network.thunder.core.database.objects.Channel;

public class ChannelCreatedResult implements ConnectionResult {

    Channel channel;

    public ChannelCreatedResult (Channel channel) {
        this.channel = channel;
    }

    @Override
    public boolean shouldTryToReconnect () {
        return true;
    }

    @Override
    public boolean wasSuccessful () {
        return false;
    }

    @Override
    public String getMessage () {
        return channel.toString();
    }

}
