package network.thunder.core.communication.objects.messages.impl.message.gossip;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.gossip.types.GossipSendMessage;
import network.thunder.core.communication.objects.p2p.P2PDataObject;

import java.util.ArrayList;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class GossipSendMessageImpl implements GossipSendMessage {
    ArrayList<P2PDataObject> dataObjects;

    @Override
    public ArrayList<P2PDataObject> getDataObjects () {
        return dataObjects;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(dataObjects);
    }
}
