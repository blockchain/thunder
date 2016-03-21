package network.thunder.core.communication.layer;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public interface MessageExecutor {
    void sendNextLayerActive ();

    void sendMessageUpwards (Message message);

    void sendMessageDownwards (Message message);

    void closeConnection ();
}
