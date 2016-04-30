package network.thunder.core.etc;

import network.thunder.core.communication.ConnectionManager;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;

import java.util.concurrent.Future;

public class MockConnectionManager implements ConnectionManager {

    @Override
    public void startListening (ResultCommand callback) {

    }

    @Override
    public void fetchNetworkIPs (ResultCommand callback) {

    }

    @Override
    public void startBuildingRandomChannel (ResultCommand callback) {

    }

    @Override
    public void connect (NodeKey node, ConnectionIntent intent, ConnectionListener connectionListener) {
        connectionListener.onSuccess.execute();
    }

    @Override
    public Future randomConnections (int amount, ConnectionIntent intent, ConnectionListener connectionListener) {
        return null;
    }

    @Override
    public void disconnectByIntent (ConnectionIntent intent) {

    }

    @Override
    public void startSyncing (ResultCommand callback) {

    }
}
