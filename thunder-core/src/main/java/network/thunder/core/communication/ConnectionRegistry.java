package network.thunder.core.communication;

public interface ConnectionRegistry {
    void onConnected (NodeKey node, Connection connection);
    void onDisconnected (NodeKey node);

    boolean isConnected (NodeKey node);
}
