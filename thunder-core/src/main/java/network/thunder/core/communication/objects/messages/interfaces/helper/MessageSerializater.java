package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.Message;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface MessageSerializater {
    byte[] serializeMessage (Message message);

    Message deserializeMessage (byte[] data);
}
