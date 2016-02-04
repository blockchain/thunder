package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedSendMessage;

import java.util.List;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public interface PeerSeedMessageFactory extends MessageFactory {

    PeerSeedSendMessage getPeerSeedSendMessage (List<PubkeyIPObject> ipObjectList);

    PeerSeedGetMessage getPeerSeedGetMessage ();
}
