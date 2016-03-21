package network.thunder.core.helper.callback.results;

public class PeerSeedResult extends ConnectionResult {

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
