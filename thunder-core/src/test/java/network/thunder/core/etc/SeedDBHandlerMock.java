package network.thunder.core.etc;

import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.communication.layer.middle.peerseed.PeerSeedProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 04/12/2015.
 */
public class SeedDBHandlerMock extends DBHandlerMock {

    public List<PubkeyIPObject> pubkeyIPObjectArrayList = new ArrayList<>();

    public SeedDBHandlerMock () {
    }

    public void fillWithRandomData () {
        for (int i = 1; i < 1000; i++) {
            PubkeyChannelObject pubkeyChannelObject = PubkeyChannelObject.getRandomObject();
            PubkeyIPObject pubkeyIPObject1 = PubkeyIPObject.getRandomObject();

            pubkeyIPObject1.pubkey = pubkeyChannelObject.pubkeyA;

            pubkeyIPObjectArrayList.add(pubkeyIPObject1);
        }
    }

    @Override
    public void insertIPObjects (List<P2PDataObject> ipList) {
        for (P2PDataObject obj : ipList) {
            if (obj instanceof PubkeyIPObject) {
                pubkeyIPObjectArrayList.add((PubkeyIPObject) obj);
            }
        }
    }

    @Override
    public List<PubkeyIPObject> getIPObjects () {
        return pubkeyIPObjectArrayList.subList(1, PeerSeedProcessor.PEERS_TO_SEND + 1);
    }
}
