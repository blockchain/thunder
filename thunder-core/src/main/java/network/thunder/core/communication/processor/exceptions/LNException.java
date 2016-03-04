package network.thunder.core.communication.processor.exceptions;

public class LNException extends RuntimeException {
    public LNException () {
        super();
    }

    public LNException (String message) {
        super(message);
    }

    public LNException (String message, Throwable cause) {
        super(message, cause);
    }

    public LNException (Throwable cause) {
        super(cause);
    }

    protected LNException (String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public boolean shouldDisconnect () {
        return false;
    }
}
