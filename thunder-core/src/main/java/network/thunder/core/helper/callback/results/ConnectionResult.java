package network.thunder.core.helper.callback.results;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public abstract class ConnectionResult implements Result {

    public abstract boolean shouldTryToReconnect ();
}
