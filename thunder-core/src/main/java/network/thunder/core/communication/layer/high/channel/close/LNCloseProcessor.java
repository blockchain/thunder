package network.thunder.core.communication.layer.high.channel.close;

import network.thunder.core.communication.layer.Processor;
import network.thunder.core.helper.callback.ResultCommand;

public abstract class LNCloseProcessor extends Processor {
    abstract public void closeChannel (int id, ResultCommand callback);
}
