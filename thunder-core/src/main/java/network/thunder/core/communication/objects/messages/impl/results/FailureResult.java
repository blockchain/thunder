package network.thunder.core.communication.objects.messages.impl.results;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.Result;

public class FailureResult implements Result {

    String message;

    public FailureResult (String message) {
        this.message = message;
    }

    public FailureResult () {
    }

    @Override
    public boolean wasSuccessful () {
        return false;
    }

    @Override
    public String getMessage () {
        return message;
    }
}
