package network.thunder.core.communication.layer;

import java.util.concurrent.Future;

public interface MessageExecutor {
    void sendNextLayerActive ();

    void sendMessageUpwards (Message message);

    void sendMessageDownwards (Message message);

    Future closeConnection ();
}
