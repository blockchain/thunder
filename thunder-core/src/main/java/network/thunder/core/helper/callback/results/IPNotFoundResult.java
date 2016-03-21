package network.thunder.core.helper.callback.results;

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
