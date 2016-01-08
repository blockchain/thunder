package network.thunder.core.communication.processor.implementations.gossip;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;

import java.util.List;

/**
 * Created by matsjerratsch on 01/12/2015.
 */
public interface GossipSubject {
    void registerObserver (NodeObserver observer);

    void removeObserver (NodeObserver observer);

//    void notifyObserver ();

    void newDataObjects (NodeObserver nodeObserver, List<P2PDataObject> dataObjects);

    List<P2PDataObject> getUpdates ();

    boolean knowsObjectAlready (byte[] hash);
}
