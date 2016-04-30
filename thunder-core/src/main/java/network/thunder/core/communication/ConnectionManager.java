package network.thunder.core.communication;

import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;

public interface ConnectionManager {
    void startListening (ResultCommand callback);

    void fetchNetworkIPs (ResultCommand callback);

    void startBuildingRandomChannel (ResultCommand callback);

    void connect (NodeKey node, ConnectionListener connectionListener);
    void disconnectByIntent (ConnectionIntent intent);

    void startSyncing (ResultCommand callback);
}
