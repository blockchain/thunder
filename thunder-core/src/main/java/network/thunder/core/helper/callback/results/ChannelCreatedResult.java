package network.thunder.core.helper.callback.results;

import network.thunder.core.communication.layer.high.Channel;

public class ChannelCreatedResult extends ConnectionResult {

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
