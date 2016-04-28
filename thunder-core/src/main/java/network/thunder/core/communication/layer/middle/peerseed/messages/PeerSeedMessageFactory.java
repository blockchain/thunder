package network.thunder.core.communication.layer.middle.peerseed.messages;

import network.thunder.core.communication.layer.MessageFactory;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;

import java.util.List;

public interface PeerSeedMessageFactory extends MessageFactory {

    PeerSeedSendMessage getPeerSeedSendMessage (List<PubkeyIPObject> ipObjectList);

    PeerSeedGetMessage getPeerSeedGetMessage ();
}
