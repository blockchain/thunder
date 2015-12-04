package network.thunder.core.communication.objects.messages.interfaces.message.sync.types;

import network.thunder.core.communication.objects.messages.interfaces.message.sync.Sync;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface SyncGetMessage extends Sync {
    int getFragmentIndex ();

    boolean getIPs ();
}
