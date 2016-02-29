package network.thunder.core.communication.processor.implementations;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedSendMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.factories.PeerSeedMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.message.peerseed.PeerSeedMessage;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.communication.processor.interfaces.PeerSeedProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public class PeerSeedProcessorImpl extends PeerSeedProcessor {

    DBHandler dbHandler;
    NodeClient node;
    NodeServer nodeServer;

    PeerSeedMessageFactory messageFactory;
    LNEventHelper eventHelper;

    MessageExecutor messageExecutor;

    public PeerSeedProcessorImpl (ContextFactory contextFactory, DBHandler dbHandler, NodeClient node) {
        this.messageFactory = contextFactory.getPeerSeedMessageFactory();
        this.dbHandler = dbHandler;
        this.eventHelper = contextFactory.getEventHelper();
        this.node = node;
        this.nodeServer = contextFactory.getServerSettings();
    }

    @Override
    public void onInboundMessage (Message message) {
        consumeMessage((PeerSeedMessage) message);
    }

    @Override
    public boolean consumesInboundMessage (Object object) {
        return (object instanceof PeerSeedMessage);
    }

    @Override
    public boolean consumesOutboundMessage (Object object) {
        return false;
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

    private void consumeMessage (PeerSeedMessage message) {
        if (message instanceof PeerSeedGetMessage) {

            List<PubkeyIPObject> ipObjects = dbHandler.getIPObjects();

            if (ipObjects.size() > PEERS_TO_SEND) {
                ipObjects = Tools.getRandomSubList(ipObjects, PEERS_TO_SEND);
            }

            Message response = messageFactory.getPeerSeedSendMessage(ipObjects);
            messageExecutor.sendMessageUpwards(response);
        } else if (message instanceof PeerSeedSendMessage) {
            PeerSeedSendMessage sendMessage = (PeerSeedSendMessage) message;
            List<PubkeyIPObject> list = removeOurIPFromList(sendMessage.ipObjectList);
            dbHandler.insertIPObjects(P2PDataObject.generaliseList(list));
            fireIPEvents(list);

            //TODO We might always want to close here, given that we only ever get here if intent = GET_IPS
            if (!node.isServer && node.intent == ChannelIntent.GET_IPS) {
                messageExecutor.closeConnection();
            }
        }
    }

    private void fireIPEvents (List<PubkeyIPObject> list) {
        for (PubkeyIPObject ip : list) {
            eventHelper.onReceivedIP(ip);
        }
    }

    private List<PubkeyIPObject> removeOurIPFromList (List<PubkeyIPObject> list) {
        List<PubkeyIPObject> toRemove = new ArrayList<>();
        for (PubkeyIPObject ip : list) {
            if (Arrays.equals(ip.pubkey, nodeServer.pubKeyServer.getPubKey())) {
                toRemove.add(ip);
            }
        }
        list.removeAll(toRemove);
        return list;
    }
}
