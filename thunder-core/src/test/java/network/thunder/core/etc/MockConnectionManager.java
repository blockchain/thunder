package network.thunder.core.etc;

import network.thunder.core.communication.ConnectionManager;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;
import network.thunder.core.helper.callback.results.SuccessResult;

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
    public void connect (NodeKey node, ConnectionListener connectionListener) {
        connectionListener.onConnection(new SuccessResult());
    }

    @Override
    public void startSyncing (ResultCommand callback) {

    }
}
