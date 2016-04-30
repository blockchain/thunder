package network.thunder.core.communication;

import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;

import java.util.concurrent.Future;

public interface ConnectionManager {
    void startListening (ResultCommand callback);

    void fetchNetworkIPs (ResultCommand callback);

    void startBuildingRandomChannel (ResultCommand callback);

    void connect (NodeKey node, ConnectionIntent intent, ConnectionListener connectionListener);
    Future randomConnections (int amount, ConnectionIntent intent, ConnectionListener connectionListener);

    void disconnectByIntent (ConnectionIntent intent);

    void startSyncing (ResultCommand callback);
}
