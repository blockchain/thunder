package network.thunder.core.communication.layer.high;

import network.thunder.core.communication.layer.Message;

public interface AckMessage extends Message {
    long getMessageNumberToAck ();
    void setMessageNumberToAck (long number);
}
