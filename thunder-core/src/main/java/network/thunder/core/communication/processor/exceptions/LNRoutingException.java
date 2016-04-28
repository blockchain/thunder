package network.thunder.core.communication.processor.exceptions;

public class LNRoutingException extends LNException {
    public LNRoutingException (String s) {
        super(s);
    }

    public LNRoutingException (Throwable cause) {
        super(cause);
    }

    @Override
    public boolean shouldDisconnect () {
        return true;
    }
}
