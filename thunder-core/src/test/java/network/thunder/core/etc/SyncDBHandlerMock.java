package network.thunder.core.etc;

import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncDBHandlerMock extends DBHandlerMock {

    public List<PubkeyChannelObject> pubkeyChannelObjectArrayList = new ArrayList<>();
    public List<PubkeyIPObject> pubkeyIPObjectArrayList = new ArrayList<>();
    public List<ChannelStatusObject> channelStatusObjectArrayList = new ArrayList<>();

    public List<P2PDataObject> totalList = new ArrayList<>();

    public Map<Integer, List<P2PDataObject>> fragmentToListMap = new HashMap<>();

    public SyncDBHandlerMock () {
    }

    public void fillWithRandomData () {
        for (int i = 0; i < 1001; i++) {
            fragmentToListMap.put(i, new ArrayList<>());
        }
        for (int i = 1; i < 1000; i++) {
            PubkeyChannelObject pubkeyChannelObject = TestTools.getRandomObjectChannelObject();
            PubkeyIPObject pubkeyIPObject1 = TestTools.getRandomObjectIpObject();
            PubkeyIPObject pubkeyIPObject2 = TestTools.getRandomObjectIpObject();
            ChannelStatusObject channelStatusObject = TestTools.getRandomObjectStatusObject();

            pubkeyIPObject1.pubkey = pubkeyChannelObject.nodeKeyA;
            pubkeyIPObject2.pubkey = pubkeyChannelObject.nodeKeyB;

            channelStatusObject.pubkeyA = pubkeyChannelObject.nodeKeyA;
            channelStatusObject.pubkeyB = pubkeyChannelObject.nodeKeyB;

            pubkeyChannelObjectArrayList.add(pubkeyChannelObject);
            pubkeyIPObjectArrayList.add(pubkeyIPObject1);
            pubkeyIPObjectArrayList.add(pubkeyIPObject2);
            channelStatusObjectArrayList.add(channelStatusObject);

            totalList.add(pubkeyChannelObject);
            totalList.add(pubkeyIPObject1);
            totalList.add(pubkeyIPObject2);
            totalList.add(channelStatusObject);

            fragmentToListMap.get(pubkeyChannelObject.getFragmentIndex()).add(pubkeyChannelObject);
            fragmentToListMap.get(pubkeyIPObject1.getFragmentIndex()).add(pubkeyIPObject1);
            fragmentToListMap.get(pubkeyIPObject2.getFragmentIndex()).add(pubkeyIPObject2);
            fragmentToListMap.get(channelStatusObject.getFragmentIndex()).add(channelStatusObject);
        }
    }

    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
        return fragmentToListMap.get(fragmentIndex);
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
