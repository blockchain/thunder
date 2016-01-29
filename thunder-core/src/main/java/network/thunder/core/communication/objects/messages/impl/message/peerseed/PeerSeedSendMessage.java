package network.thunder.core.communication.objects.messages.impl.message.peerseed;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.message.peerseed.PeerSeedMessage;

import java.util.List;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public class PeerSeedSendMessage implements PeerSeedMessage {

    public List<PubkeyIPObject> ipObjectList;

    public PeerSeedSendMessage (List<PubkeyIPObject> ipObjectList) {
        this.ipObjectList = ipObjectList;
    }

    @Override
    public void verify () {

    }

    @Override
    public String toString () {
        return "PeerSeedSendMessage{" +
                "ipObjectList=" + ipObjectList +
                '}';
    }
}
