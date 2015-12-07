package network.thunder.core.communication.objects.messages.impl.message.gossip;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.gossip.Gossip;

import java.util.ArrayList;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class GossipInvMessage implements Gossip {
    public ArrayList<byte[]> inventoryList;

    public GossipInvMessage (ArrayList<byte[]> inventoryList) {
        this.inventoryList = inventoryList;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(inventoryList);
    }
}
