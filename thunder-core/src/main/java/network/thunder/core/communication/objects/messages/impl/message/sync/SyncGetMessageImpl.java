package network.thunder.core.communication.objects.messages.impl.message.sync;

import network.thunder.core.communication.objects.messages.interfaces.message.sync.types.SyncGetMessage;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class SyncGetMessageImpl implements SyncGetMessage {
    int fragmentIndex;
    boolean getIPs;

    @Override
    public int getFragmentIndex () {
        return fragmentIndex;
    }

    @Override
    public boolean getIPs () {
        return getIPs;
    }

    @Override
    public void verify () {

    }
}
