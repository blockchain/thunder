package network.thunder.core.communication.layer;

public interface MessageFactory {
    FailureMessage getFailureMessage (String failure);

    Message parseMessage (Object object);

}
