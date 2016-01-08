package network.thunder.core.communication.processor.implementations.sync;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;

import java.util.*;

/**
 * Structure for synchronizing new nodes.
 * <p>
 * While it is not important the new node gets a 100% consistent view of the network, it should still be fairly accurate.
 * If the topography after the complete sync is not good enough, we can always add further queries to it.
 */
public class SynchronizationHelper {
    private final static int NUMBER_OF_NODE_TO_SYNC_FROM = 10;

    private DBHandler dbHandler;

    private Map<Integer, ArrayList<P2PDataObject>> fragmentList = new HashMap<>();
    private List<P2PDataObject> fullDataList = new ArrayList<>();
    private Set<P2PDataObject> fullDataListSet = new HashSet<>();

    private Map<Integer, Boolean> fragmentIsSyncedList = new HashMap<>();
    private Map<Integer, Integer> fragmentJobList = new HashMap<>();

    private List<PubkeyIPObject> ipObjectList = new ArrayList<>();

    private boolean fullySynchronized = false;

    public SynchronizationHelper (DBHandler dbHandler) {
        this.dbHandler = dbHandler;
        for (int i = 1; i < P2PDataObject.NUMBER_OF_FRAGMENTS + 1; i++) {
            ArrayList<P2PDataObject> objList = new ArrayList<>();
            fragmentList.put(i, objList);
            fragmentIsSyncedList.put(i, false);
            fragmentJobList.put(i, 0);
        }
    }

    public synchronized int getNextFragmentIndexToSynchronize () {
        for (int i = 1; i < P2PDataObject.NUMBER_OF_FRAGMENTS + 1; i++) {
            if (!fragmentIsSyncedList.get(i)) {
                if ((Tools.currentTime() - fragmentJobList.get(i)) > 60) { //Give each fragment 60s to sync..
                    fragmentJobList.put(i, Tools.currentTime());
                    return i;
                }
            }
        }
        return 0;
    }

    public void newFragment (int index, List<P2PDataObject> newFragment) {
        for (P2PDataObject object : newFragment) {
            verifyDataObject(index, object);
            insertDataObjectInLists(object);
        }
        fragmentIsSyncedList.put(index, true);
    }

    public void newIPList (List<P2PDataObject> ipList) {
        for (P2PDataObject ip : ipList) {
            if (ip instanceof PubkeyIPObject) {
                ipObjectList.add((PubkeyIPObject) ip);
            }
        }
        dbHandler.insertIPObjects(ipList);
    }

    private boolean verifyDataObject (int index, P2PDataObject object) {
        try {
            //TODO: Probably a bottleneck here, maybe we can trust the data?
            if (index != object.getFragmentIndex()) {
                System.out.println("Object should not be in that index.. Is in: " + index + " Should be: " + object.getFragmentIndex());
            }
            object.verify();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void insertDataObjectInLists (P2PDataObject object) {
        ArrayList<P2PDataObject> objectArrayList = fragmentList.get(object.getFragmentIndex());
        if (!objectArrayList.contains(object)) {
            objectArrayList.add(object);
        }
        if (!fullDataListSet.contains(object)) {
            fullDataList.add(object);
        }
    }

    public boolean fullySynchronized () {
        if (fullySynchronized) {
            return true;
        }
        for (int i = 1; i < P2PDataObject.NUMBER_OF_FRAGMENTS + 1; i++) {
            if (!fragmentIsSyncedList.get(i)) {
                return false;
            }
        }
        fullySynchronized = true;
        return true;
    }

    public void saveFullSyncToDatabase () {

        System.out.println("Received all sync data...");

        //TODO: Validate all anchors we received. We need some kind of full blockchain to do that..

        dbHandler.syncDatalist(fullDataList);
    }

    public List<P2PDataObject> getFragment (int index) {
        return dbHandler.getSyncDataByFragmentIndex(index);
    }

    public List<P2PDataObject> getIPAddresses () {
        return dbHandler.getSyncDataIPObjects();
    }

}
