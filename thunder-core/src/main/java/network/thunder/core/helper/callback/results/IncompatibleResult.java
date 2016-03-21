package network.thunder.core.helper.callback.results;

import network.thunder.core.communication.layer.FailureMessage;

public class IncompatibleResult extends ConnectionResult {
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
