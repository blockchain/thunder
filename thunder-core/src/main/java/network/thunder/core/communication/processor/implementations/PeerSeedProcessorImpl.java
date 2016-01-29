package network.thunder.core.communication.processor.implementations;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedSendMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.PeerSeedMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.peerseed.PeerSeedMessage;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.communication.processor.interfaces.PeerSeedProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.mesh.Node;

import java.util.List;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public class PeerSeedProcessorImpl extends PeerSeedProcessor {

    PeerSeedMessageFactory messageFactory;
    DBHandler dbHandler;
    Node node;

    MessageExecutor messageExecutor;

    public PeerSeedProcessorImpl (PeerSeedMessageFactory messageFactory, DBHandler dbHandler, Node node) {
        this.messageFactory = messageFactory;
        this.dbHandler = dbHandler;
        this.node = node;
    }

    @Override
    public void onInboundMessage (Message message) {
        if (message instanceof PeerSeedMessage) {
            consumeMessage((PeerSeedMessage) message);
        } else {
            messageExecutor.sendMessageDownwards(message);
        }
    }

    @Override
    public void onOutboundMessage (Message message) {
        messageExecutor.sendMessageUpwards(message);
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
        if (node.isServer) {
            messageExecutor.sendNextLayerActive();
        } else {
            if (node.intent == ChannelIntent.GET_IPS) {
                messageExecutor.sendMessageUpwards(messageFactory.getPeerSeedGetMessage());
            } else {
                messageExecutor.sendNextLayerActive();
            }
        }
    }

    public void consumeMessage (PeerSeedMessage message) {
        if (message instanceof PeerSeedGetMessage) {
            List<PubkeyIPObject> ipObjects = dbHandler.getIPObjects();
            Message response = messageFactory.getPeerSeedSendMessage(ipObjects);
            messageExecutor.sendMessageUpwards(response);
        } else if (message instanceof PeerSeedSendMessage) {
            PeerSeedSendMessage sendMessage = (PeerSeedSendMessage) message;
            dbHandler.insertIPObjects(P2PDataObject.generaliseList(sendMessage.ipObjectList));

            //TODO We might always want to close here, given that we only ever get here if intent = GET_IPS
            if (!node.isServer && node.intent == ChannelIntent.GET_IPS) {
                messageExecutor.closeConnection();
            }
        }
    }
}
