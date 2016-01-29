package network.thunder.core.etc;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.ChannelStatusObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyChannelObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.lightning.RevocationHash;
import network.thunder.core.mesh.Node;

import java.util.*;

/**
 * Created by matsjerratsch on 04/12/2015.
 */
public class InMemoryDBHandlerMock implements DBHandler {

    public List<PubkeyIPObject> pubkeyIPList = new ArrayList<>();
    public List<PubkeyChannelObject> pubkeyChannelList = new ArrayList<>();
    public List<ChannelStatusObject> channelStatusList = new ArrayList<>();

    public Map<Integer, List<P2PDataObject>> fragmentToListMap = new HashMap<>();

    public List<P2PDataObject> totalList = new ArrayList<>();

    public List<PubkeyIPObject> pubkeyIPObjectOpenChannel = new ArrayList<>();

    public List<RevocationHash> revocationHashListTheir = new ArrayList<>();
    public List<RevocationHash> revocationHashListOurs = new ArrayList<>();

    public InMemoryDBHandlerMock () {
        for (int i = 0; i < 1001; i++) {
            fragmentToListMap.put(i, new ArrayList<>());
        }
    }

    @Override
    public void insertIPObjects (List<P2PDataObject> ipList) {
        syncDatalist(ipList);
    }

    @Override
    public List<PubkeyIPObject> getIPObjects () {
        return new ArrayList<>(pubkeyIPList);
    }

    @Override
    public P2PDataObject getP2PDataObjectByHash (byte[] hash) {
        for (P2PDataObject object : totalList) {
            if (Arrays.equals(object.getHash(), hash)) {
                return object;
            }
        }
        return null;
    }

    @Override
    public void syncDatalist (List<P2PDataObject> dataList) {
        for (P2PDataObject obj : dataList) {
            if (obj instanceof PubkeyIPObject) {
                if (!pubkeyIPList.contains(obj)) {
                    pubkeyIPList.add((PubkeyIPObject) obj);
                }
            } else if (obj instanceof PubkeyChannelObject) {
                if (!pubkeyChannelList.contains(obj)) {
                    pubkeyChannelList.add((PubkeyChannelObject) obj);
                }
            } else if (obj instanceof ChannelStatusObject) {
                if (!channelStatusList.contains(obj)) {
                    channelStatusList.add((ChannelStatusObject) obj);
                }
            }
            List<P2PDataObject> list = getSyncDataByFragmentIndex(obj.getFragmentIndex());
            if (!list.contains(obj)) {
                list.add(obj);
            }
            if (!totalList.contains(obj)) {
                totalList.add(obj);
            }
        }
    }

    @Override
    public void insertRevocationHash (RevocationHash hash) {
        revocationHashListTheir.add(hash);
    }

    @Override
    public RevocationHash createRevocationHash (Channel channel) {
        RevocationHash hash = new RevocationHash(1, 1, Tools.getRandomByte(32));
        revocationHashListOurs.add(hash);
        return hash;
    }

    @Override
    public List<RevocationHash> getOldRevocationHashes (Channel channel) {
        List<RevocationHash> hashList = new ArrayList<>(revocationHashListOurs);
        revocationHashListOurs.clear();
        return hashList;
    }

    @Override
    public boolean checkOldRevocationHashes (List<RevocationHash> revocationHashList) {
        //TODO
        return true;
    }

    @Override
    public Channel getChannel (Node node) {
        //TODO
        return null;
    }

    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
        return fragmentToListMap.get(fragmentIndex);
    }

    @Override
    public List<P2PDataObject> getSyncDataIPObjects () {
        return null;
    }

    @Override
    public List<PubkeyIPObject> getIPObjectsWithActiveChannel () {
        return new ArrayList<>();
    }

    private void pruneLists () {
        List<PubkeyIPObject> oldList = new ArrayList<>();

        for (PubkeyIPObject oldIP : pubkeyIPList) {
            for (PubkeyIPObject newIP : pubkeyIPList) {
                if (!oldIP.equals(newIP)) {
                    if (Arrays.equals(oldIP.pubkey, newIP.pubkey) && (oldIP.timestamp < newIP.timestamp)) {
                        oldList.add(oldIP);
                    }
                }
            }
        }

        pubkeyIPList.removeAll(oldList);
    }
}
