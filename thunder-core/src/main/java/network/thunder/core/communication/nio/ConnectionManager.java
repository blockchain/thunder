package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ResultCommand;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public interface ConnectionManager {
    void startUp (ResultCommand callback) throws Exception;

    void startListening (ResultCommand callback);

    void fetchNetworkIPs (ResultCommand callback);

    void startBuildingRandomChannel (ResultCommand callback);

    void buildChannel (byte[] nodeKey, ResultCommand callback);
}
