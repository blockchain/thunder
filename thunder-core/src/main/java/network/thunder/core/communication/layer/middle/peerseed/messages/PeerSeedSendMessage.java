package network.thunder.core.communication.layer.middle.peerseed.messages;

import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;

import java.util.List;

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
