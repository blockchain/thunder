package network.thunder.core.communication.objects.messages.impl.message.gossip;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.gossip.Gossip;
import network.thunder.core.etc.Tools;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class GossipGetMessage implements Gossip {
    public List<byte[]> inventoryList;

    public GossipGetMessage (List<byte[]> inventoryList) {
        this.inventoryList = inventoryList;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(inventoryList);
    }

    @Override
    public String toString () {
        return "GossipGetMessage{" +
                "size = " + inventoryList.size() + " - " + Tools.bytesToHex(inventoryList.get(0)) +
                '}';
    }
}
