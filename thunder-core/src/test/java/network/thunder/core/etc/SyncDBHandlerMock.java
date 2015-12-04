package network.thunder.core.etc;

import network.thunder.core.communication.objects.p2p.P2PDataObject;
import network.thunder.core.communication.objects.p2p.sync.ChannelStatusObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyChannelObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyIPObject;
import network.thunder.core.database.DBHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 04/12/2015.
 */
public class SyncDBHandlerMock implements DBHandler {

    public List<PubkeyChannelObject> pubkeyChannelObjectArrayList = new ArrayList<>();
    public List<PubkeyIPObject> pubkeyIPObjectArrayList = new ArrayList<>();
    public List<ChannelStatusObject> channelStatusObjectArrayList = new ArrayList<>();

    public List<P2PDataObject> totalList = new ArrayList<>();

    public SyncDBHandlerMock () {
    }

    public void fillWithRandomData () {
        for (int i = 1; i < 100; i++) {
            PubkeyChannelObject pubkeyChannelObject = PubkeyChannelObject.getRandomObject();
            PubkeyIPObject pubkeyIPObject1 = PubkeyIPObject.getRandomObject();
            PubkeyIPObject pubkeyIPObject2 = PubkeyIPObject.getRandomObject();
            ChannelStatusObject channelStatusObject = ChannelStatusObject.getRandomObject();

            pubkeyIPObject1.pubkey = pubkeyChannelObject.pubkeyA;
            pubkeyIPObject2.pubkey = pubkeyChannelObject.pubkeyB;

            channelStatusObject.pubkeyA = pubkeyChannelObject.pubkeyA;
            channelStatusObject.pubkeyB = pubkeyChannelObject.pubkeyB;

            pubkeyChannelObjectArrayList.add(pubkeyChannelObject);
            pubkeyIPObjectArrayList.add(pubkeyIPObject1);
            pubkeyIPObjectArrayList.add(pubkeyIPObject2);
            channelStatusObjectArrayList.add(channelStatusObject);

            totalList.add(pubkeyChannelObject);
            totalList.add(pubkeyIPObject1);
            totalList.add(pubkeyIPObject2);
            totalList.add(channelStatusObject);
        }
    }

    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
        List<P2PDataObject> dataObjectList = new ArrayList<>();
        for (P2PDataObject object : totalList) {
            if (object.getFragmentIndex() == fragmentIndex) {
                dataObjectList.add(object);
            }
        }
        return dataObjectList;
    }

    @Override
    public List<P2PDataObject> getSyncDataIPObjects () {
        List<P2PDataObject> ipList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ipList.add(pubkeyIPObjectArrayList.get(i));
        }
        return ipList;
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
    public P2PDataObject getP2PDataObjectByHash (byte[] hash) {
        return null;
    }

    @Override
    public void syncDatalist (List<P2PDataObject> dataList) {
        for (P2PDataObject obj : dataList) {
            if (obj instanceof PubkeyIPObject) {
                pubkeyIPObjectArrayList.add((PubkeyIPObject) obj);
            } else if (obj instanceof PubkeyChannelObject) {
                pubkeyChannelObjectArrayList.add((PubkeyChannelObject) obj);
            } else if (obj instanceof ChannelStatusObject) {
                channelStatusObjectArrayList.add((ChannelStatusObject) obj);
            }
            totalList.add(obj);
        }

    }
}
