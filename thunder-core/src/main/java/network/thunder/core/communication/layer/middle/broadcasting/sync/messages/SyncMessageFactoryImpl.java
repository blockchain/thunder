package network.thunder.core.communication.layer.middle.broadcasting.sync.messages;

import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.MesssageFactoryImpl;

import java.util.List;

public class SyncMessageFactoryImpl extends MesssageFactoryImpl implements SyncMessageFactory {
    @Override
    public SyncGetMessage getSyncGetMessage (int fragment) {
        return new SyncGetMessage(fragment);
    }

    @Override
    public SyncSendMessage getSyncSendMessage (List<P2PDataObject> dataObjects) {
        return new SyncSendMessage(dataObjects);
    }
}
