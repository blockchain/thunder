package network.thunder.core.communication.processor.exceptions;

/**
 * Created by matsjerratsch on 11/01/2016.
 */
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
