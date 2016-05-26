package network.thunder.core.communication.layer.high;

import network.thunder.core.communication.layer.Message;

public interface NumberedMessage extends Message {
    long getMessageNumber ();
    void setMessageNumber (long number);
}
