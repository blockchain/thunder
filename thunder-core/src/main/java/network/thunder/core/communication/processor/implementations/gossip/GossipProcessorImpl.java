package network.thunder.core.communication.processor.implementations.gossip;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipInvMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipSendMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.factories.GossipMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.gossip.Gossip;
import network.thunder.core.communication.processor.interfaces.GossipProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GossipProcessorImpl extends GossipProcessor {

    GossipMessageFactory messageFactory;
    GossipSubject subject;
    DBHandler dbHandler;
    int portServer;
    String hostnameServer;
    Node node;

    MessageExecutor messageExecutor;

    List<P2PDataObject> objectList = new ArrayList<>();
    List<P2PDataObject> objectListTemp = new ArrayList<>();

    public GossipProcessorImpl (GossipMessageFactory messageFactory, GossipSubject subject, DBHandler dbHandler, int portServer, String hostnameServer, Node
            node) {
        this.messageFactory = messageFactory;
        this.subject = subject;
        this.dbHandler = dbHandler;
        this.portServer = portServer;
        this.hostnameServer = hostnameServer;
        this.node = node;
    }

    @Override
    public void onInboundMessage (Message message) {
        if (message instanceof Gossip) {
            consumeMessage(message);
        } else {
            messageExecutor.sendMessageDownwards(message);
        }
    }

    @Override
    public void onOutboundMessage (Message message) {

    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
        subject.registerObserver(this);

        sendOwnIPAddress();
    }

    @Override
    public void onLayerClose () {
        subject.removeObserver(this);
    }

    @Override
    public void update (List<P2PDataObject> newObjectList) {
        for (P2PDataObject object : newObjectList) {
            if (!objectList.contains(object)) {
                objectList.add(object);
            }
        }
        sendInvMessage();
    }

    private void consumeMessage (Message message) {
        if (message instanceof GossipInvMessage) {
            processGossipInvMessage(message);
        } else if (message instanceof GossipSendMessage) {
            processGossipSendMessage(message);
        } else if (message instanceof GossipGetMessage) {
            processGossipGetMessage(message);
        } else {
            throw new UnsupportedOperationException("Don't know this Gossip Message: " + message);
        }
    }

    private void processGossipInvMessage (Message message) {
        GossipInvMessage invMessage = (GossipInvMessage) message;
        List<byte[]> objectsToGet = new ArrayList<>();
        for (byte[] hash : invMessage.inventoryList) {
            if (!subject.knowsObjectAlready(hash)) {
                objectsToGet.add(hash);
            }
        }
        if (objectsToGet.size() > 0) {
            sendGossipGetMessage(objectsToGet);
        }
    }

    private void processGossipSendMessage (Message message) {
        GossipSendMessage sendMessage = (GossipSendMessage) message;
        subject.newDataObjects(this, sendMessage.dataObjects);
    }

    private void processGossipGetMessage (Message message) {
        GossipGetMessage getMessage = (GossipGetMessage) message;
        sendGossipSendMessage(getMessage.inventoryList);
    }

    private void sendInvMessage () {
        if (objectList.size() > OBJECT_AMOUNT_TO_SEND) {
            List<byte[]> hashList = translateP2PDataObjectListToHashList(objectList);
            Message invMessage = messageFactory.getGossipInvMessage(hashList);
            messageExecutor.sendMessageUpwards(invMessage);
            swapLists();
        }
    }

    private void sendOwnIPAddress () {
        PubkeyIPObject pubkeyIPObject = new PubkeyIPObject();
        pubkeyIPObject.pubkey = node.pubKeyServer.getPubKey();
        pubkeyIPObject.port = this.portServer;
        pubkeyIPObject.IP = this.hostnameServer;
        pubkeyIPObject.timestamp = Tools.currentTime();
        pubkeyIPObject.sign(node.pubKeyServer);

        List<P2PDataObject> ipAddresses = new ArrayList<>();
        ipAddresses.add(pubkeyIPObject);
        Message gossipSendMessage = messageFactory.getGossipSendMessage(ipAddresses);
        messageExecutor.sendMessageUpwards(gossipSendMessage);

    }

    private void sendGossipSendMessage (List<byte[]> objectsToSend) {
        List<P2PDataObject> listToSend = buildObjectList(objectsToSend);
        Message gossipSendMessage = messageFactory.getGossipSendMessage(listToSend);
        messageExecutor.sendMessageUpwards(gossipSendMessage);
    }

    private void sendGossipGetMessage (List<byte[]> objectsToGet) {
        Message gossipGetMessage = messageFactory.getGossipGetMessage(objectsToGet);
        messageExecutor.sendMessageUpwards(gossipGetMessage);
    }

    private List<P2PDataObject> buildObjectList (List<byte[]> objectsToSend) {
        List<P2PDataObject> listToSend = new ArrayList<>();
        for (byte[] hash : objectsToSend) {
            addP2PObjectToList(hash, listToSend);
        }
        return listToSend;
    }

    private void addP2PObjectToList (byte[] hash, List<P2PDataObject> objectListToAdd) {
        for (P2PDataObject object : objectList) {
            if (Arrays.equals(object.getHash(), hash)) {
                objectListToAdd.add(object);
                return;
            }
        }

        for (P2PDataObject object : objectListTemp) {
            if (Arrays.equals(object.getHash(), hash)) {
                objectListToAdd.add(object);
                return;
            }
        }

        P2PDataObject object = dbHandler.getP2PDataObjectByHash(hash);
        if (object != null) {
            objectListToAdd.add(object);
            return;
        }

    }

    private void swapLists () {
        objectListTemp.clear();
        objectListTemp.addAll(objectList);
        objectList.clear();
    }

    private List<byte[]> translateP2PDataObjectListToHashList (List<P2PDataObject> objectList) {
        List<byte[]> hashList = new ArrayList<>();
        for (P2PDataObject object : objectList) {
            hashList.add(object.getHash());
        }
        return hashList;
    }

}
