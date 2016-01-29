package network.thunder.core.communication.processor;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;

/**
 * Created by matsjerratsch on 27/11/2015.
 */
public abstract class Processor {
    public abstract void onInboundMessage (Message message);

    public abstract void onOutboundMessage (Message message);

    public abstract void onLayerActive (MessageExecutor messageExecutor);

    public void onLayerClose () {
        
    }

}
