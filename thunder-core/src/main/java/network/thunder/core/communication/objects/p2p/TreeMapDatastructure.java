package network.thunder.core.communication.objects.p2p;

import network.thunder.core.etc.Tools;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Structure for synchronizing new nodes.
 * <p>
 * While it is not important the new node gets a 100% consistent view of the network, it should still be fairly accurate.
 * If the topography after the complete sync is not good enough, we can always add further queries to it.
 */
public class TreeMapDatastructure {

    public final static int NUMBER_OF_FRAGMENTS = 1000;
    private final static long FRAGMENT_SIZE = Long.MAX_VALUE / NUMBER_OF_FRAGMENTS;

    public HashMap<Integer, ArrayList<P2PDataObject>> list = new HashMap<>();
    public ArrayList<P2PDataObject> fullDataList = new ArrayList<>();

    public HashMap<Integer, Boolean> fragmentIsSyncedList = new HashMap<>();
    public HashMap<Integer, Integer> fragmentJobList = new HashMap<>();

    public TreeMapDatastructure () {
        for (int i = 1; i < NUMBER_OF_FRAGMENTS; i++) {
            ArrayList<P2PDataObject> objList = new ArrayList<>();
            list.put(i, objList);
            fragmentIsSyncedList.put(i, false);
            fragmentJobList.put(i, 0);
        }
    }

    public ArrayList<P2PDataObject> getFragment (int index) {
        return list.get(index);
    }

    public int objectToFragmentIndex (P2PDataObject object) {

        return (int) (object.getHash() / FRAGMENT_SIZE + 1);
    }

    public void insertObject (P2PDataObject obj) {
        list.get(objectToFragmentIndex(obj)).add(obj);
    }

    public boolean contains (P2PDataObject obj) {
        return list.get(objectToFragmentIndex(obj)).contains(obj);
    }

    public void newFragment (int index, ArrayList<P2PDataObject> newFragment) {

        ArrayList<P2PDataObject> objectArrayList = list.get(index);

        for (P2PDataObject object : newFragment) {
            //TODO: Probably a bottleneck here, maybe we can trust the data?
            if (index != objectToFragmentIndex(object)) {
                throw new RuntimeException("Object should not be in that index..");
            }

            if (!objectArrayList.contains(object)) {
                objectArrayList.add(object);
            }
            if (!fullDataList.contains(object)) {
                fullDataList.add(object);
            }
        }
        fragmentIsSyncedList.put(index, true);
    }

    public synchronized int getNextFragmentIndexToSynchronize () {
        for (int i = 1; i < NUMBER_OF_FRAGMENTS; i++) {
            if (!fragmentIsSyncedList.get(i)) {
                if ((Tools.currentTime() - fragmentJobList.get(i)) > 60) { //Give each fragment 60s to sync..
                    fragmentJobList.put(i, Tools.currentTime());
                    return i;
                }
            }
        }
        return 0;
    }

    public boolean fullySynchronized () {
        for (int i = 1; i < NUMBER_OF_FRAGMENTS; i++) {
            if (!fragmentIsSyncedList.get(i)) {
                return false;
            }
        }
        return true;
    }

}
