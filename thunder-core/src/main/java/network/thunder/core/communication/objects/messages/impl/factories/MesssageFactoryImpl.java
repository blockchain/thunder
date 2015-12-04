package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.message.FailureMessageImpl;
import network.thunder.core.communication.objects.messages.interfaces.factories.MessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.FailureMessage;

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
