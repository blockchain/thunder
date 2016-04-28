package network.thunder.core.communication.layer.middle.broadcasting.gossip;

import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.helper.events.LNEventHelper;

import java.nio.ByteBuffer;
import java.util.*;

public class GossipSubjectImpl implements GossipSubject, BroadcastHelper {

    DBHandler dbHandler;
    LNEventHelper eventHelper;

    final List<NodeObserver> observerList = Collections.synchronizedList(new ArrayList<>());

    Map<NodeObserver, List<ByteBuffer>> dataObjectMap = new HashMap<>();
    Set<ByteBuffer> objectsKnownAlready = new HashSet<>();

    public GossipSubjectImpl (DBHandler dbHandler, LNEventHelper eventHelper) {
        this.dbHandler = dbHandler;
        this.eventHelper = eventHelper;
    }

    @Override
    public void registerObserver (NodeObserver observer) {
        synchronized (observerList) {
            observerList.add(observer);
            dataObjectMap.put(observer, new ArrayList<>());
        }
    }

    @Override
    public void removeObserver (NodeObserver observer) {
        synchronized (observerList) {
            observerList.remove(observer);
            dataObjectMap.remove(observer);
        }
    }

    @Override
    public void receivedNewObjects (NodeObserver nodeObserver, List<P2PDataObject> dataObjects) {
        List<P2PDataObject> objectsToInsertIntoDatabase = new ArrayList<>();
        for (P2PDataObject dataObject : dataObjects) {
            boolean newEntry = insertNewDataObject(nodeObserver, dataObject);
            if (newEntry) {
                objectsToInsertIntoDatabase.add(dataObject);
            }
        }
        dbHandler.syncDatalist(objectsToInsertIntoDatabase);
        eventHelper.onP2PDataReceived();
        broadcast();
    }

    @Override
    public boolean parseInv (NodeObserver nodeObserver, byte[] hash) {
        ByteBuffer b = ByteBuffer.wrap(hash);
        dataObjectMap.get(nodeObserver).remove(b);
        return objectsKnownAlready.contains(ByteBuffer.wrap(hash));
    }

    //Brand new object that should be broadcasted to all peers..
    //Part of BroadcastHelper
    @Override
    public void broadcastNewObject (P2PDataObject dataObject) {
        List<P2PDataObject> wrapper = new ArrayList<>();
        wrapper.add(dataObject);
        dbHandler.syncDatalist(wrapper);
        insertNewDataObject(null, dataObject);
        broadcast();
    }

    private boolean insertNewDataObject (NodeObserver nodeObserver, P2PDataObject dataObject) {
        if (objectsKnownAlready.add(ByteBuffer.wrap(dataObject.getHash()))) {
            addNewDataObjectToMap(nodeObserver, dataObject);
            return true;
        }
        return false;
    }

    private void broadcast () {
        synchronized (observerList) {
            for (NodeObserver observer : observerList) {
                List<ByteBuffer> objectList = dataObjectMap.get(observer);
                if (objectList.size() > GossipProcessor.OBJECT_AMOUNT_TO_SEND) {
                    observer.update(new ArrayList<>(objectList));
                    objectList.clear();
                }
            }
        }
    }

    private void addNewDataObjectToMap (NodeObserver nodeObserver, P2PDataObject dataObject) {
        synchronized (observerList) {
            for (NodeObserver nodeObserver1 : observerList) {
                if (nodeObserver == null || nodeObserver != nodeObserver1) {
                    List<ByteBuffer> bufferList = dataObjectMap.get(nodeObserver1);
                    ByteBuffer buffer = ByteBuffer.wrap(dataObject.getHash());
                    if (!bufferList.contains(buffer)) {
                        bufferList.add(buffer);
                    }
                }
            }
        }
    }
}
