package network.thunder.core.communication.layer;

/**
 * Created by matsjerratsch on 27/11/2015.
 */
public interface MessageFactory {
    FailureMessage getFailureMessage (String failure);

    Message parseMessage (Object object);

}
