package network.thunder.core.communication.layer.low.serialisation;

import network.thunder.core.communication.layer.Message;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface MessageSerializer {
    byte[] serializeMessage (Message message);

    Message deserializeMessage (byte[] data);
}
