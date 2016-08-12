package network.thunder.core.communication.layer.low.authentication.messages;

import network.thunder.core.communication.layer.Message;

public abstract class Authentication implements Message {
    @Override
    public String getMessageType () {
        return "Authentication";
    }
}
