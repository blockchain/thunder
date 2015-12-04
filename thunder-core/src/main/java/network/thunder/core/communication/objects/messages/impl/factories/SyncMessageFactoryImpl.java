package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.sync.SyncGetMessageImpl;
import network.thunder.core.communication.objects.messages.impl.message.sync.SyncSendMessageImpl;
import network.thunder.core.communication.objects.messages.interfaces.factories.SyncMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.sync.types.SyncGetMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.sync.types.SyncSendMessage;
import network.thunder.core.communication.objects.p2p.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 04/12/2015.
 */
public class SyncMessageFactoryImpl extends MesssageFactoryImpl implements SyncMessageFactory {
    @Override
    public SyncGetMessage getSyncGetMessage (int fragment) {
        return new SyncGetMessageImpl(fragment, false);
    }

    @Override
    public SyncGetMessage getSyncSendIPMessage () {
        return new SyncGetMessageImpl(0, true);
    }

    @Override
    public SyncSendMessage getSyncSendMessage (List<P2PDataObject> dataObjects) {
        return new SyncSendMessageImpl(dataObjects);
    }
}
