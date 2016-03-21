package network.thunder.core.helper.callback.results;

public class SyncSuccessResult extends ConnectionResult {
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
        return null;
    }
}
