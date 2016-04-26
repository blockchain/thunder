package network.thunder.core.communication;

import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public interface ConnectionManager {
    void startUp (ResultCommand callback) throws Exception;

    void startListening (ResultCommand callback);

    void fetchNetworkIPs (ResultCommand callback);

    void startBuildingRandomChannel (ResultCommand callback);

    void connect (NodeKey node, ConnectionListener connectionListener);

    void startSyncing (ResultCommand callback);
}
