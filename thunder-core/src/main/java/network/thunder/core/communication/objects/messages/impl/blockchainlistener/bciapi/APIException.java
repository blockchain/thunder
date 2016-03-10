package network.thunder.core.communication.objects.messages.impl.blockchainlistener.bciapi;

/**
 * The class `APIException` represents a failed call to the Blockchain API. Whenever
 * the server is unable to process a request (usually due to parameter validation errors),
 * an instance of this class is thrown.
 */
public class APIException extends Exception {
    private static final long serialVersionUID = -7731961787745059713L;

    public APIException (String message) {
        super(message);
    }
}
