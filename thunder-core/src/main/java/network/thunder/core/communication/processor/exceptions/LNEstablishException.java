package network.thunder.core.communication.processor.exceptions;

public class LNEstablishException extends LNException {
    public LNEstablishException (String s) {
        super(s);
    }

    public LNEstablishException (Throwable cause) {
        super(cause);
    }

    @Override
    public boolean shouldDisconnect () {
        return true;
    }
}
