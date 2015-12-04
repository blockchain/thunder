package network.thunder.core.communication.objects.p2p;

import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Structure for synchronizing new nodes.
 * <p>
 * While it is not important the new node gets a 100% consistent view of the network, it should still be fairly accurate.
 * If the topography after the complete sync is not good enough, we can always add further queries to it.
 */
public class SynchronizationHelper {

    public final static int NUMBER_OF_FRAGMENTS = 1000;

    private final static int NUMBER_OF_NODE_TO_SYNC_FROM = 10;

    private final static long FRAGMENT_SIZE = Long.MAX_VALUE / NUMBER_OF_FRAGMENTS;

    private HashMap<Integer, ArrayList<P2PDataObject>> list = new HashMap<>();
    private ArrayList<P2PDataObject> fullDataList = new ArrayList<>();

    private HashMap<Integer, Boolean> fragmentIsSyncedList = new HashMap<>();
    private HashMap<Integer, Integer> fragmentJobList = new HashMap<>();

    private boolean fullySynchronized = false;

    private DBHandler dbHandler;

    public SynchronizationHelper () {
        for (int i = 1; i < NUMBER_OF_FRAGMENTS + 1; i++) {
            ArrayList<P2PDataObject> objList = new ArrayList<>();
            list.put(i, objList);
            fragmentIsSyncedList.put(i, false);
            fragmentJobList.put(i, 0);
        }
    }

    public static int objectToFragmentIndex (P2PDataObject object) {

        return (int) (object.getHashAsLong() / FRAGMENT_SIZE + 1);
    }

    public boolean contains (P2PDataObject obj) {
        return list.get(objectToFragmentIndex(obj)).contains(obj);
    }

    public boolean fullySynchronized () {
        if (fullySynchronized()) {
            return fullySynchronized;
        }
        for (int i = 1; i < NUMBER_OF_FRAGMENTS + 1; i++) {
//            if(i>100) return true;
            if (!fragmentIsSyncedList.get(i)) {
                return false;
            }
        }
        fullySynchronized = true;
        return true;
    }

    public ArrayList<P2PDataObject> getFragment (int index) {
        return list.get(index);
    }

    public synchronized int getNextFragmentIndexToSynchronize () {
        for (int i = 1; i < NUMBER_OF_FRAGMENTS + 1; i++) {
//            if(i>100) return 0;
            if (!fragmentIsSyncedList.get(i)) {
                if ((Tools.currentTime() - fragmentJobList.get(i)) > 60) { //Give each fragment 60s to sync..
                    fragmentJobList.put(i, Tools.currentTime());
                    return i;
                }
            }
        }
        return 0;
    }

    public void insertObject (P2PDataObject obj) {
        list.get(objectToFragmentIndex(obj)).add(obj);
    }

    public void newFragment (int index, List<P2PDataObject> newFragment) {

        ArrayList<P2PDataObject> objectArrayList = list.get(index);

        for (P2PDataObject object : newFragment) {
            //TODO: Probably a bottleneck here, maybe we can trust the data?
            if (index != objectToFragmentIndex(object)) {
//                throw new RuntimeException("Object should not be in that index..");
                System.out.println("Object should not be in that index.. Is in: " + index + " Should be: " + objectToFragmentIndex(object));
            }

            //Check the signature on all objects we get before processing..
            object.verify();

            if (!objectArrayList.contains(object)) {
                objectArrayList.add(object);
            }
//            if (!fullDataList.contains(object)) {
            fullDataList.add(object);
//            }

            //Save all data into our database
//            System.out.println(object);
//            ChannelStatusObject status = (ChannelStatusObject) object;

        }

        fragmentIsSyncedList.put(index, true);
    }

    public void saveFullSyncToDatabase () {


        System.out.println("Received all sync data...");

        //TODO: Validate all anchors we received. We need some kind of full blockchain to do that..

        //Save all data into our database
        dbHandler.syncDatalist( fullDataList);

        //Now figure out to which nodes we want to build lightning channels..
    }

}
