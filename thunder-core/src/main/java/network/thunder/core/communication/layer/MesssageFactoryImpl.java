package network.thunder.core.communication.layer;

/**
 * Created by matsjerratsch on 27/11/2015.
 */
public abstract class MesssageFactoryImpl implements MessageFactory {
    @Override
    public FailureMessage getFailureMessage (String failure) {
        return new FailureMessageImpl(failure);
    }

    @Override
    public Message parseMessage (Object object) {
        return null;
    }
}
