package network.thunder.core.communication.objects.messages.impl.results;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ConnectionResult;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ResultCommand;

public class NullResultCommand implements ResultCommand {
    @Override
    public void execute (ConnectionResult result) {

    }
}
