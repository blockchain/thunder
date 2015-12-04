package network.thunder.core.communication.objects.messages.interfaces.message.gossip.types;

import network.thunder.core.communication.objects.messages.interfaces.message.gossip.Gossip;

import java.util.ArrayList;

/**
 * Created by matsjerratsch on 26/10/2015.
 */
public interface GossipGetMessage extends Gossip {
    ArrayList<byte[]> getInventoryList ();

}
