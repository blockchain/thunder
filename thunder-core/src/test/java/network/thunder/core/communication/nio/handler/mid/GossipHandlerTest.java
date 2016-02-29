package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.impl.factories.GossipMessageFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipInvMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipSendMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyChannelObject;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.factories.GossipMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.processor.implementations.gossip.GossipProcessorImpl;
import network.thunder.core.communication.processor.interfaces.GossipProcessor;
import network.thunder.core.etc.InMemoryDBHandler;
import network.thunder.core.etc.MockContextFactory;
import network.thunder.core.etc.MockLNEventHelper;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.junit.After;
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

/**
 * Created by matsjerratsch on 27/10/2015.
 */
public class GossipHandlerTest {

    NodeClient node1 = new NodeClient();
    NodeClient node2 = new NodeClient();
    NodeClient node3 = new NodeClient();
    NodeClient node4 = new NodeClient();

    NodeServer nodeServer1 = new NodeServer();
    NodeServer nodeServer2 = new NodeServer();
    NodeServer nodeServer3 = new NodeServer();
    NodeServer nodeServer4 = new NodeServer();

    InMemoryDBHandler dbHandler1 = new InMemoryDBHandler();
    InMemoryDBHandler dbHandler2 = new InMemoryDBHandler();
    InMemoryDBHandler dbHandler3 = new InMemoryDBHandler();
    InMemoryDBHandler dbHandler4 = new InMemoryDBHandler();

    ContextFactory contextFactory1 = new MockContextFactory(nodeServer1, dbHandler1);
    ContextFactory contextFactory2 = new MockContextFactory(nodeServer2, dbHandler2);
    ContextFactory contextFactory3 = new MockContextFactory(nodeServer3, dbHandler3);
    ContextFactory contextFactory4 = new MockContextFactory(nodeServer4, dbHandler4);

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

    @After
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

        channel12.checkException();
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

    }

    @Test
    public void shouldSendDataToNextPeer () throws Exception {
        List<P2PDataObject> dataList = new ArrayList<>();
        for (int i = 0; i < GossipProcessor.OBJECT_AMOUNT_TO_SEND + 100; i++) {
            dataList.add(PubkeyChannelObject.getRandomObject());
        }
        GossipSendMessage sendDataObject = messageFactory.getGossipSendMessage(dataList);

        channel21.writeInbound(sendDataObject);

        channel32.writeInbound(channel23.readOutbound());
        channel23.writeInbound(channel32.readOutbound());
        channel32.writeInbound(channel23.readOutbound());

        channel43.writeInbound(channel34.readOutbound());
        channel34.writeInbound(channel43.readOutbound());
        channel43.writeInbound(channel34.readOutbound());

        assertTrue(dbHandler2.totalList.containsAll(dataList));
        assertTrue(dbHandler3.totalList.containsAll(dataList));
        assertTrue(dbHandler4.totalList.containsAll(dataList));
    }

    private void clearOutput () {
        channel12.readOutbound();
        channel21.readOutbound();
        channel23.readOutbound();
        channel32.readOutbound();
        channel34.readOutbound();
        channel43.readOutbound();
    }

    public boolean checkListsEqual (List list1, List list2) {
        return list1.containsAll(list2) && list2.containsAll(list1);
    }

    private void prepareNodes () {
        prepareNode(nodeServer1);
        prepareNode(nodeServer2);
        prepareNode(nodeServer3);
        prepareNode(nodeServer4);
    }

    private void prepareNode (NodeServer node) {
        node.init();
        node.portServer = new Random().nextInt(65555);
        node.hostServer = "localhost";
    }

}