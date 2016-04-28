package network.thunder.core.communication.layer.middle.peerseed.messages;

import network.thunder.core.communication.layer.MesssageFactoryImpl;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;

import java.util.List;

public class PeerSeedMessageFactoryImpl extends MesssageFactoryImpl implements PeerSeedMessageFactory {

    @Override
    public PeerSeedSendMessage getPeerSeedSendMessage (List<PubkeyIPObject> ipObjectList) {
        return new PeerSeedSendMessage(ipObjectList);
    }

    @Override
    public PeerSeedGetMessage getPeerSeedGetMessage () {
        return new PeerSeedGetMessage();
    }
}
