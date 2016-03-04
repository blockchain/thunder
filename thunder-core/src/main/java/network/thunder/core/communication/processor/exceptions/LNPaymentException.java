package network.thunder.core.communication.processor.exceptions;

/**
 * Created by matsjerratsch on 11/01/2016.
 */
public class LNPaymentException extends LNException {
    public LNPaymentException (String s) {
        super(s);
    }

    public LNPaymentException (Throwable cause) {
        super(cause);
    }

    @Override
    public boolean shouldDisconnect () {
        return true;
    }
}
