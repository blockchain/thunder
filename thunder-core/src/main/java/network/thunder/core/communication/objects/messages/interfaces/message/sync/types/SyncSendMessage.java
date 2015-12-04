package network.thunder.core.communication.objects.messages.interfaces.message.sync.types;

import network.thunder.core.communication.objects.messages.interfaces.message.sync.Sync;
import network.thunder.core.communication.objects.p2p.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface SyncSendMessage extends Sync {
    List<P2PDataObject> getSyncData ();
}
