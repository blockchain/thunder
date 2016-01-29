package network.thunder.core.database;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.lightning.RevocationHash;
import network.thunder.core.mesh.Node;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface DBHandler {
    List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex);

    List<P2PDataObject> getSyncDataIPObjects ();

    void insertIPObjects (List<P2PDataObject> ipList);

    List<PubkeyIPObject> getIPObjects ();

    P2PDataObject getP2PDataObjectByHash (byte[] hash);

    void syncDatalist (List<P2PDataObject> dataList);

    void insertRevocationHash (RevocationHash hash);

    RevocationHash createRevocationHash (Channel channel);

    List<RevocationHash> getOldRevocationHashes (Channel channel);

    boolean checkOldRevocationHashes (List<RevocationHash> revocationHashList);

    Channel getChannel (Node node);

    List<PubkeyIPObject> getIPObjectsWithActiveChannel ();
}
