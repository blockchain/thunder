package network.thunder.core.communication.layer.high.channel.close;

import network.thunder.core.communication.processor.exceptions.LNException;

public class LNCloseException extends LNException {
    public LNCloseException (String s) {
        super(s);
    }

    public LNCloseException (Throwable cause) {
        super(cause);
    }

    @Override
    public boolean shouldDisconnect () {
        return true;
    }
}
