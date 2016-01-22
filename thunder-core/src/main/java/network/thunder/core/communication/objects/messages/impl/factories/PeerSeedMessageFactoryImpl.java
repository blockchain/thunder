package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedSendMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.PeerSeedMessageFactory;

import java.util.List;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
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
