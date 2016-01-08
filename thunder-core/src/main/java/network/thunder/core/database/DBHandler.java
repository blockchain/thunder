package network.thunder.core.database;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface DBHandler {
    List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex);

    List<P2PDataObject> getSyncDataIPObjects ();

    void insertIPObjects (List<P2PDataObject> ipList);

    P2PDataObject getP2PDataObjectByHash (byte[] hash);

    void syncDatalist (List<P2PDataObject> dataList);
}
