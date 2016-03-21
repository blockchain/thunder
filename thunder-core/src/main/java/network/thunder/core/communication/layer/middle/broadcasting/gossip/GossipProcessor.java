package network.thunder.core.communication.layer.middle.broadcasting.gossip;

import network.thunder.core.communication.layer.Processor;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.NodeObserver;

/**
 * Created by matsjerratsch on 01/12/2015.
 */
public abstract class GossipProcessor extends Processor implements NodeObserver {
    public final static int OBJECT_AMOUNT_TO_SEND = 0;

}
