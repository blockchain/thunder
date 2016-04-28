package network.thunder.core.communication.layer;

public interface MessageExecutor {
    void sendNextLayerActive ();

    void sendMessageUpwards (Message message);

    void sendMessageDownwards (Message message);

    void closeConnection ();
}
