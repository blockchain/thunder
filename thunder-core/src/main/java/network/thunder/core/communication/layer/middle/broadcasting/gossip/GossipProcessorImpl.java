package network.thunder.core.communication.layer.middle.broadcasting.gossip;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.messages.*;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GossipProcessorImpl extends GossipProcessor {

    GossipMessageFactory messageFactory;
    GossipSubject subject;
    DBHandler dbHandler;
    ClientObject node;
    ServerObject serverObject;

    MessageExecutor messageExecutor;

    boolean firstMessage = true;

    int randomNumber;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public GossipProcessorImpl (ContextFactory contextFactory, DBHandler dbHandler, ClientObject
            node) {
        this.messageFactory = contextFactory.getGossipMessageFactory();
        this.subject = contextFactory.getGossipSubject();
        this.dbHandler = dbHandler;
        this.node = node;
        this.serverObject = contextFactory.getServerSettings();
    }

    @Override
    public void onInboundMessage (Message message) {
        consumeMessage(message);
    }

    @Override
    public boolean consumesInboundMessage (Object object) {
        return object instanceof Gossip;
    }

    @Override
    public boolean consumesOutboundMessage (Object object) {
        return false;
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.randomNumber = new SecureRandom().nextInt();
        this.messageExecutor = messageExecutor;
        subject.registerObserver(this);

        startScheduledBroadcasting();
        messageExecutor.sendNextLayerActive();
    }

    @Override
    public void onLayerClose () {
        subject.removeObserver(this);
        scheduler.shutdown();
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

        //TODO: Hack to show the other hostname in debugs
        if (firstMessage && sendMessage.pubkeyIPList.size() > 0) {
            node.port = sendMessage.pubkeyIPList.get(0).port;
            node.host = sendMessage.pubkeyIPList.get(0).hostname;
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
        if (serverObject.hostServer == null || serverObject.hostServer.equals("")) {
            return;
        }
        PubkeyIPObject pubkeyIPObject = new PubkeyIPObject();
        pubkeyIPObject.pubkey = serverObject.pubKeyServer.getPubKey();
        pubkeyIPObject.port = serverObject.portServer;
        pubkeyIPObject.hostname = serverObject.hostServer;
        pubkeyIPObject.timestamp = Tools.currentTime();
        pubkeyIPObject.sign(serverObject.pubKeyServer);

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

    private void startScheduledBroadcasting () {
        sendOwnIPAddress();
        int time = (int) (P2PDataObject.MAXIMUM_AGE_SYNC_DATA * 0.75);
        scheduler.scheduleAtFixedRate((Runnable) () -> sendOwnIPAddress(), Tools.getRandom(0, time), time, TimeUnit.SECONDS);
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
