package network.thunder.core.communication;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public interface ConnectionRegistry {
    void onConnected (NodeKey node);
    void onDisconnected (NodeKey node);

    boolean isConnected (NodeKey node);
}
