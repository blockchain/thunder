package network.thunder.core.helper.callback.results;

public abstract class ConnectionResult implements Result {

    public abstract boolean shouldTryToReconnect ();
}
