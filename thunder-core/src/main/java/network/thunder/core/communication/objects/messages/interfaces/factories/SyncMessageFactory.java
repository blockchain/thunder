package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.impl.message.sync.SyncGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.sync.SyncSendMessage;
import network.thunder.core.communication.objects.p2p.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface SyncMessageFactory extends MessageFactory {
    SyncGetMessage getSyncGetMessage (int fragment);

    SyncGetMessage getSyncSendIPMessage ();

    SyncSendMessage getSyncSendMessage (List<P2PDataObject> dataObjects);

}
