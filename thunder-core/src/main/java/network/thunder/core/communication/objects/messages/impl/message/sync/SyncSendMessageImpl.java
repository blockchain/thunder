package network.thunder.core.communication.objects.messages.impl.message.sync;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.sync.types.SyncSendMessage;
import network.thunder.core.communication.objects.p2p.P2PDataObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class SyncSendMessageImpl implements SyncSendMessage {
    ArrayList<P2PDataObject> dataObjects;

    @Override
    public List<P2PDataObject> getSyncData () {
        return dataObjects;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(dataObjects);
    }
}
