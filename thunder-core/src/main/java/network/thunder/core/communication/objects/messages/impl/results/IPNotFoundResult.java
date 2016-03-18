package network.thunder.core.communication.objects.messages.impl.results;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ConnectionResult;

public class IPNotFoundResult extends ConnectionResult {
    @Override
    public boolean shouldTryToReconnect () {
        return false;
    }

    @Override
    public boolean wasSuccessful () {
        return false;
    }

    @Override
    public String getMessage () {
        return "IP not found..";
    }
}
