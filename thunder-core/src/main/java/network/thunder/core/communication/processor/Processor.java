package network.thunder.core.communication.processor;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;

/**
 * Created by matsjerratsch on 27/11/2015.
 */
public interface Processor {
    public void onInboundMessage (Message message);

    public void onOutboundMessage (Message message);

    public void onLayerActive (MessageExecutor messageExecutor);

}
