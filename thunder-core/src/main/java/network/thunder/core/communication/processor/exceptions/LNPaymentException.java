package network.thunder.core.communication.processor.exceptions;

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
