package network.thunder.core.communication.objects.messages.impl.message.sync;

import network.thunder.core.communication.objects.messages.interfaces.message.sync.Sync;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class SyncGetMessage implements Sync {
    public int fragmentIndex;
    public boolean getIPs;

    public SyncGetMessage (boolean getIPs, int fragmentIndex) {
        this.getIPs = getIPs;
        this.fragmentIndex = fragmentIndex;
    }

    @Override
    public void verify () {

    }
}
