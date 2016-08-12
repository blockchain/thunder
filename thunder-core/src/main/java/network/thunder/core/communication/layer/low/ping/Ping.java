package network.thunder.core.communication.layer.low.ping;

import network.thunder.core.communication.layer.Message;

public class Ping implements Message {
    @Override
    public void verify () {

    }

    @Override
    public String getMessageType () {
        return "Ping";
    }
}
