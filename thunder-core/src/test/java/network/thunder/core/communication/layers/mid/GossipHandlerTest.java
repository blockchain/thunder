package network.thunder.core.communication.layers.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.ProcessorHandler;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.GossipProcessor;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.GossipProcessorImpl;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.messages.*;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.database.InMemoryDBHandler;
import network.thunder.core.etc.MockContextFactory;
import network.thunder.core.etc.MockLNEventHelper;
import network.thunder.core.helper.events.LNEventHelper;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

public class GossipHandlerTest {

    ClientObject node1 = new ClientObject();
    ClientObject node2 = new ClientObject();
    ClientObject node3 = new ClientObject();
    ClientObject node4 = new ClientObject();

    ServerObject serverObject1 = new ServerObject();
    ServerObject serverObject2 = new ServerObject();
    ServerObject serverObject3 = new ServerObject();
    ServerObject serverObject4 = new ServerObject();

    InMemoryDBHandler dbHandler1 = new InMemoryDBHandler();
    InMemoryDBHandler dbHandler2 = new InMemoryDBHandler();
    InMemoryDBHandler dbHandler3 = new InMemoryDBHandler();
    InMemoryDBHandler dbHandler4 = new InMemoryDBHandler();

    ContextFactory contextFactory1 = new MockContextFactory(serverObject1, dbHandler1);
    ContextFactory contextFactory2 = new MockContextFactory(serverObject2, dbHandler2);
    ContextFactory contextFactory3 = new MockContextFactory(serverObject3, dbHandler3);
    ContextFactory contextFactory4 = new MockContextFactory(serverObject4, dbHandler4);

    GossipMessageFactory messageFactory;

    //Weird naming convention here, because we have channels between two nodes
    //TODO Refactor
    GossipProcessor gossipProcessor12;
    GossipProcessor gossipProcessor21;
    GossipProcessor gossipProcessor23;
    GossipProcessor gossipProcessor32;
    GossipProcessor gossipProcessor34;
    GossipProcessor gossipProcessor43;

    EmbeddedChannel channel12;
    EmbeddedChannel channel21;
    EmbeddedChannel channel23;
    EmbeddedChannel channel32;
    EmbeddedChannel channel34;
    EmbeddedChannel channel43;

    LNEventHelper eventHelper = new MockLNEventHelper();

    @Before
    public void prepare () throws PropertyVetoException, SQLException {

        prepareNodes();

        node1.isServer = false;
        node2.isServer = true;
        node3.isServer = true;
        node4.isServer = true;
        node1.name = "Gossip1";
        node2.name = "Gossip2";
        node3.name = "Gossip3";
        node4.name = "Gossip4";

        messageFactory = new GossipMessageFactoryImpl();
        messageFactory = new GossipMessageFactoryImpl();
        messageFactory = new GossipMessageFactoryImpl();
        messageFactory = new GossipMessageFactoryImpl();

        gossipProcessor12 = new GossipProcessorImpl(contextFactory1, dbHandler1, node1);

        gossipProcessor21 = new GossipProcessorImpl(contextFactory2, dbHandler2, node2);
        gossipProcessor23 = new GossipProcessorImpl(contextFactory2, dbHandler2, node2);

        gossipProcessor32 = new GossipProcessorImpl(contextFactory3, dbHandler3, node3);
        gossipProcessor34 = new GossipProcessorImpl(contextFactory3, dbHandler3, node3);

        gossipProcessor43 = new GossipProcessorImpl(contextFactory4, dbHandler4, node4);

        channel12 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor12, "Gossip12"));

        channel21 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor21, "Gossip21"));
        channel23 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor23, "Gossip23"));

        channel32 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor32, "Gossip32"));
        channel34 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor34, "Gossip34"));

        channel43 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor43, "Gossip43"));

        clearOutput();
    }

    public void after () {
        channel12.checkException();
        channel21.checkException();

        channel21.checkException();
        channel23.checkException();

        channel32.checkException();
        channel34.checkException();

        channel43.checkException();
    }

    @Test
    public void shouldAskForDataAfterInv () throws Exception {
        ArrayList<byte[]> invList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            byte[] b = new byte[20];
            new Random().nextBytes(b);
            invList.add(b);
        }

        GossipInvMessage message = messageFactory.getGossipInvMessage(invList);

        channel12.writeInbound(message);

        Message message1 = (Message) channel12.readOutbound();
        assertThat(message1, instanceOf(GossipGetMessage.class));

        GossipGetMessage getMessage = (GossipGetMessage) message1;

        for (int i = 0; i < invList.size(); i++) {
            assertTrue(Arrays.equals(getMessage.inventoryList.get(i), invList.get(i)));
        }

        after();
    }

    @Test
    public void shouldOnlySendDataOnceEnoughObjectsArrived () throws Exception {
        if (GossipProcessor.OBJECT_AMOUNT_TO_SEND == 0) {
            //TODO currently we need all gossip objects immediately, will be fixed with RP routing
            return;
        }
        List<P2PDataObject> dataList = new ArrayList<>();
        for (int i = 0; i < GossipProcessor.OBJECT_AMOUNT_TO_SEND + 1; i++) {
            dataList.add(PubkeyChannelObject.getRandomObject());
        }

        List<P2PDataObject> dataList1 = dataList.subList(0, GossipProcessor.OBJECT_AMOUNT_TO_SEND - 10);

        channel21.writeInbound(messageFactory.getGossipSendMessage(dataList1));
        assertNull(channel23.readOutbound());

        channel21.writeInbound(messageFactory.getGossipSendMessage(dataList));

        GossipInvMessage invMessage = (GossipInvMessage) channel23.readOutbound();

        assertNotNull(invMessage);
        assertTrue(invMessage.inventoryList.size() == dataList.size());

        after();
    }

    @Test
    public void shouldSendDataToNextPeer () throws Exception {
        List<P2PDataObject> dataList = new ArrayList<>();
        for (int i = 0; i < GossipProcessor.OBJECT_AMOUNT_TO_SEND + 100; i++) {
            dataList.add(PubkeyChannelObject.getRandomObject());
        }
        GossipSendMessage sendDataObject = messageFactory.getGossipSendMessage(dataList);

        channel21.writeInbound(sendDataObject);

        sendMessage(channel23, channel32);
        sendMessage(channel32, channel23);
        sendMessage(channel23, channel32);

        Thread.sleep(200);

        sendMessage(channel34, channel43);
        sendMessage(channel43, channel34);
        sendMessage(channel34, channel43);

        assertTrue(dbHandler2.totalList.containsAll(dataList));
        assertTrue(dbHandler3.totalList.containsAll(dataList));
        assertTrue(dbHandler4.totalList.containsAll(dataList));
        after();
    }

    private void clearOutput () {
        channel12.readOutbound();
        channel21.readOutbound();
        channel23.readOutbound();
        channel32.readOutbound();
        channel34.readOutbound();
        channel43.readOutbound();
    }

    public void sendMessage (EmbeddedChannel from, EmbeddedChannel to) {
        Message m = (Message) from.readOutbound();
        to.writeInbound(m);
    }

    public boolean checkListsEqual (List list1, List list2) {
        return list1.containsAll(list2) && list2.containsAll(list1);
    }

    private void prepareNodes () {
        prepareNode(serverObject1);
        prepareNode(serverObject2);
        prepareNode(serverObject3);
        prepareNode(serverObject4);
    }

    private void prepareNode (ServerObject node) {
        node.init();
        node.portServer = new Random().nextInt(65555);
        node.hostServer = "localhost";
    }

}