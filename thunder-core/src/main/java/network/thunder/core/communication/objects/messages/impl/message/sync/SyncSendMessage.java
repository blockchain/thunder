package network.thunder.core.communication.objects.messages.impl.message.sync;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.sync.Sync;
import network.thunder.core.communication.objects.p2p.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class SyncSendMessage implements Sync {
    public List<P2PDataObject> dataObjects;

    public SyncSendMessage (List<P2PDataObject> dataObjects) {
        this.dataObjects = dataObjects;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(dataObjects);
    }
}
