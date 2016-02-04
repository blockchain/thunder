package network.thunder.core.communication.objects.messages.interfaces.message;

import network.thunder.core.communication.Message;

/**
 * Created by matsjerratsch on 27/11/2015.
 */
public interface FailureMessage extends Message {
    String getFailure ();
}
