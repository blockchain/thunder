package network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish;

import network.thunder.core.communication.Message;
import network.thunder.core.database.objects.Channel;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public interface LNEstablish extends Message {
    public void saveToChannel (Channel channel);
}
