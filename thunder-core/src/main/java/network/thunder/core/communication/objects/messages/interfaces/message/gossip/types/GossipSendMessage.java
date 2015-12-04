package network.thunder.core.communication.objects.messages.interfaces.message.gossip.types;

import network.thunder.core.communication.objects.messages.interfaces.message.gossip.Gossip;
import network.thunder.core.communication.objects.p2p.P2PDataObject;

import java.util.ArrayList;

/**
 * Created by matsjerratsch on 26/10/2015.
 */
public interface GossipSendMessage extends Gossip {
    ArrayList<P2PDataObject> getDataObjects ();
}
