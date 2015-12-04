package network.thunder.core.communication.objects.messages;

import network.thunder.core.communication.Message;

/**
 * Created by matsjerratsch on 29/11/2015.
 */
public interface MessageExecutor {
    void sendNextLayerActive ();

    void sendMessageUpwards (Message message);

    void sendMessageDownwards (Message message);

    void closeConnection ();
}
