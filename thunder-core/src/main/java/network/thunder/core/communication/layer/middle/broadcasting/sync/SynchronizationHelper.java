package network.thunder.core.communication.layer.middle.broadcasting.sync;

import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.SyncListener;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Structure for synchronizing new nodes.
 * <p>
 * While it is not important the new node gets a 100% consistent view of the network, it should still be fairly accurate.
 * If the topography after the complete sync is not good enough, we can always add further queries to it.
 */
public class SynchronizationHelper {
    private static final Logger log = Tools.getLogger();

    public final static int TIME_TO_SYNC_ONE_FRAGMENT = 1000;
    private final static int MAXIMUM_TRIES_SYNCING = P2PDataObject.NUMBER_OF_FRAGMENTS * 50;
    private DBHandler dbHandler;

    private Map<Integer, List<P2PDataObject>> fragmentList = new HashMap<>();
    private List<P2PDataObject> fullDataList = new ArrayList<>();
    private Set<P2PDataObject> fullDataListSet = new HashSet<>();

    private Map<Integer, Boolean> fragmentIsSyncedList = new HashMap<>();
    private Map<Integer, Long> fragmentJobList = new HashMap<>();

    private boolean fullySynchronized = false;

    final List<SyncClient> syncClientList = Collections.synchronizedList(new ArrayList<>());
    Map<SyncClient, Integer> currentlySyncing = new HashMap<>();

    private SyncListener syncListener;

    private ExecutorService executorService = new ThreadPoolExecutor(1, 1, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

    public SynchronizationHelper (DBHandler dbHandler) {
        this.dbHandler = dbHandler;
        initialise();
    }

    public void addSyncClient (SyncClient syncClient) {
        synchronized (syncClientList) {
            syncClientList.add(syncClient);
        }
    }

    public void removeSyncClient (SyncClient syncClient) {
        synchronized (syncClientList) {
            syncClientList.remove(syncClient);
        }
    }

    private synchronized int getNextFragmentIndexToSynchronize () {
        for (int i = 1; i < P2PDataObject.NUMBER_OF_FRAGMENTS + 1; i++) {
            if (!fragmentIsSyncedList.get(i)) {
                if ((Tools.currentTime() - fragmentJobList.get(i)) > TIME_TO_SYNC_ONE_FRAGMENT) {
                    fragmentJobList.put(i, System.currentTimeMillis());
                    return i;
                }
            }
        }
        return 0;
    }

    public Future resync (SyncListener syncListener) {
        this.syncListener = syncListener;
        initialise();
        return executorService.submit((Runnable) () -> {
            try {
                sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void initialise () {
        for (int i = 1; i < P2PDataObject.NUMBER_OF_FRAGMENTS + 1; i++) {
            fragmentList.put(i, new ArrayList<>());
            fragmentIsSyncedList.put(i, false);
            fragmentJobList.put(i, (long) 0);
        }
        fullySynchronized = false;
    }

    private void sync () throws InterruptedException {
        if (syncClientList.size() == 0) {
            syncListener.onSyncFailed();
            return;
        }
        int counter = 0;
        while (!fullySynchronized()) {
            Thread.sleep(100);
            synchronized (syncClientList) {
                for (SyncClient syncClient : syncClientList) {
                    Integer currentSegment = currentlySyncing.get(syncClient);
                    if (currentSegment != null) {
                        Boolean segmentSynced = fragmentIsSyncedList.get(currentSegment);
                        if (segmentSynced == null || segmentSynced == Boolean.FALSE) {
                            long time = System.currentTimeMillis() - fragmentJobList.get(currentSegment);
                            if (time < TIME_TO_SYNC_ONE_FRAGMENT) {
                                continue;
                            }
                        }
                    }
                    int nextFragment = getNextFragmentIndexToSynchronize();
                    if (nextFragment == 0) {
                        continue;
                    }
                    counter++;
                    currentlySyncing.put(syncClient, nextFragment);
                    syncClient.syncSegment(nextFragment);
                }
            }

            if (counter > MAXIMUM_TRIES_SYNCING) {
                syncListener.onSyncFailed();
                return;
            }
        }
        saveFullSyncToDatabase();
        syncListener.onSyncFinished();
    }

    public void newFragment (int index, List<P2PDataObject> newFragment) {
        for (P2PDataObject object : newFragment) {
            verifyDataObject(index, object);
            insertDataObjectInLists(object);
        }
        dbHandler.syncDatalist(newFragment);
        fragmentIsSyncedList.put(index, true);

    }

    private boolean verifyDataObject (int index, P2PDataObject object) {
        try {
            //TODO: Probably a bottleneck here, maybe we can trust the data?
            if (index != object.getFragmentIndex()) {
                log.error("Object should not be in that index.. Is in: " + index + " Should be: " + object.getFragmentIndex());
            }
            object.verify();
            return true;
        } catch (Exception e) {
            log.warn("", e);
        }
        return false;
    }

    private void insertDataObjectInLists (P2PDataObject object) {
        List<P2PDataObject> objectArrayList = fragmentList.get(object.getFragmentIndex());
        if (!objectArrayList.contains(object)) {
            objectArrayList.add(object);
        }
        if (!fullDataListSet.add(object)) {
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

    private void saveFullSyncToDatabase () {
        //TODO: Validate all anchors we received. We need some kind of full blockchain to do that..

        dbHandler.syncDatalist(fullDataList);
    }

    public List<P2PDataObject> getFragment (int index) {
        return dbHandler.getSyncDataByFragmentIndex(index);
    }
}
