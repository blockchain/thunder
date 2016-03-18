package network.thunder.core.communication.objects.messages.impl.results;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.Result;

public class SuccessResult implements Result {
    @Override
    public boolean wasSuccessful () {
        return true;
    }

    @Override
    public String getMessage () {
        return null;
    }
}
