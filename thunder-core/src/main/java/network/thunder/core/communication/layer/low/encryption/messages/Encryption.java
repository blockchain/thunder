package network.thunder.core.communication.layer.low.encryption.messages;

import network.thunder.core.communication.layer.Message;

public abstract class Encryption implements Message {
    @Override
    public String getMessageType () {
        return "Encryption";
    }
}
