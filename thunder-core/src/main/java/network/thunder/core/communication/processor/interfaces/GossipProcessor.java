package network.thunder.core.communication.processor.interfaces;

import network.thunder.core.communication.processor.Processor;
import network.thunder.core.communication.processor.implementations.gossip.NodeObserver;

/**
 * Created by matsjerratsch on 01/12/2015.
 */
public interface GossipProcessor extends Processor, NodeObserver {
    public final static int OBJECT_AMOUNT_TO_SEND = 50;

}
