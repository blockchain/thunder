package network.thunder.core.communication.objects.messages.impl.message.peerseed;

import network.thunder.core.communication.objects.messages.interfaces.message.peerseed.PeerSeedMessage;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public class PeerSeedGetMessage implements PeerSeedMessage {
    @Override
    public void verify () {

    }

    @Override
    public String toString () {
        return "PeerSeedGetMessage{}";
    }
}
