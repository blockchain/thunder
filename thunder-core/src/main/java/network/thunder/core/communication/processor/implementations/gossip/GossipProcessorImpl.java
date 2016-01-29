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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GossipProcessorImpl extends GossipProcessor {

    GossipMessageFactory messageFactory;
    GossipSubject subject;
    DBHandler dbHandler;
    Node node;

    MessageExecutor messageExecutor;

    boolean firstMessage = true;

    int randomNumber;

    public GossipProcessorImpl (GossipMessageFactory messageFactory, GossipSubject subject, DBHandler dbHandler, Node
            node) {
        this.messageFactory = messageFactory;
        this.subject = subject;
        this.dbHandler = dbHandler;
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
        this.messageExecutor.sendMessageUpwards(message);
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.randomNumber = new Random().nextInt();
        this.messageExecutor = messageExecutor;
        subject.registerObserver(this);

        sendOwnIPAddress();
        messageExecutor.sendNextLayerActive();
    }

    @Override
    public void onLayerClose () {
        subject.removeObserver(this);
    }

    @Override
    public void update (List<ByteBuffer> newObjectList) {
        sendInvMessage(newObjectList);
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
            if (!subject.parseInv(this, hash)) {
                objectsToGet.add(hash);
            }
        }
        if (objectsToGet.size() > 0) {
            sendGossipGetMessage(objectsToGet);
        }
    }

    private void processGossipSendMessage (Message message) {
        GossipSendMessage sendMessage = (GossipSendMessage) message;
        subject.receivedNewObjects(this, sendMessage.getDataList());

        if (firstMessage) {
            node.port = sendMessage.pubkeyIPList.get(0).port;
            firstMessage = false;
        }
    }

    private void processGossipGetMessage (Message message) {
        GossipGetMessage getMessage = (GossipGetMessage) message;
        sendGossipSendMessage(getMessage.inventoryList);
    }

    private synchronized void sendInvMessage (List<ByteBuffer> invList) {
        Message invMessage = messageFactory.getGossipInvMessage(Tools.byteBufferListToByteArrayList(invList));
        messageExecutor.sendMessageUpwards(invMessage);
    }

    private void sendOwnIPAddress () {
        PubkeyIPObject pubkeyIPObject = new PubkeyIPObject();
        pubkeyIPObject.pubkey = node.pubKeyServer.getPubKey();
        pubkeyIPObject.port = node.portServer;
        pubkeyIPObject.IP = node.hostServer;
        pubkeyIPObject.timestamp = Tools.currentTimeFlooredToCurrentDay();
        pubkeyIPObject.sign(node.pubKeyServer);

        List<P2PDataObject> ipAddresses = new ArrayList<>();
        ipAddresses.add(pubkeyIPObject);
        Message gossipSendMessage = messageFactory.getGossipSendMessage(ipAddresses);
        messageExecutor.sendMessageUpwards(gossipSendMessage);

        //Hack to register the object we sent out here..
        subject.receivedNewObjects(this, ipAddresses);

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
        //TODO optimize using a Cache somewhere..

        P2PDataObject object = dbHandler.getP2PDataObjectByHash(hash);
        if (object != null) {
            objectListToAdd.add(object);
        }

    }

    private List<byte[]> translateP2PDataObjectListToHashList (List<P2PDataObject> objectList) {
        List<byte[]> hashList = new ArrayList<>();
        for (P2PDataObject object : objectList) {
            hashList.add(object.getHash());
        }
        return hashList;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GossipProcessorImpl that = (GossipProcessorImpl) o;

        return randomNumber == that.randomNumber;

    }

    @Override
    public int hashCode () {
        return randomNumber;
    }
}
