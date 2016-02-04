package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.sync.SyncGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.sync.SyncSendMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.SyncMessageFactory;

import java.util.List;

/**
 * Created by matsjerratsch on 04/12/2015.
 */
public class SyncMessageFactoryImpl extends MesssageFactoryImpl implements SyncMessageFactory {
    @Override
    public SyncGetMessage getSyncGetMessage (int fragment) {
        return new SyncGetMessage(false, fragment);
    }

    @Override
    public SyncGetMessage getSyncSendIPMessage () {
        return new SyncGetMessage(true, 0);
    }

    @Override
    public SyncSendMessage getSyncSendMessage (List<P2PDataObject> dataObjects) {
        return new SyncSendMessage(dataObjects);
    }
}
