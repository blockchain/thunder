package network.thunder.core.communication.objects.messages.impl.results;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ConnectionResult;

public class PeerSeedResult implements ConnectionResult {

    @Override
    public boolean shouldTryToReconnect () {
        return false;
    }

    @Override
    public boolean wasSuccessful () {
        return true;
    }

    @Override
    public String getMessage () {
        return "";
    }

}
