package network.thunder.core.communication.layer.low.serialisation;

import network.thunder.core.communication.layer.Message;

public interface MessageSerializer {
    byte[] serializeMessage (Message message);

    Message deserializeMessage (byte[] data);
}
