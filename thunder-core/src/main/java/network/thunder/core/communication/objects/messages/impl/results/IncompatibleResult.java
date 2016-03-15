package network.thunder.core.communication.objects.messages.impl.results;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ConnectionResult;
import network.thunder.core.communication.objects.messages.interfaces.message.FailureMessage;

public class IncompatibleResult implements ConnectionResult {
    FailureMessage failureMessage;

    @Override
    public boolean shouldTryToReconnect () {
        return false;
    }

    @Override
    public boolean wasSuccessful () {
        return false;
    }

    @Override
    public String getMessage () {
        return failureMessage.getFailure();
    }
}
