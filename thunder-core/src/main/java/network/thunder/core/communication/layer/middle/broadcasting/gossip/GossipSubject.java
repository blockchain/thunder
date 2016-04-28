package network.thunder.core.communication.layer.middle.broadcasting.gossip;

import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;

import java.util.List;

public interface GossipSubject {
    void registerObserver (NodeObserver observer);

    void removeObserver (NodeObserver observer);

    void receivedNewObjects (NodeObserver nodeObserver, List<P2PDataObject> dataObjects);

    boolean parseInv (NodeObserver nodeObservers, byte[] hash);
}
