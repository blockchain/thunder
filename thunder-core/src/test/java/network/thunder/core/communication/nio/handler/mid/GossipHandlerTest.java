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
import network.thunder.core.communication.objects.messages.interfaces.factories.GossipMessageFactory;
import network.thunder.core.communication.processor.implementations.gossip.GossipProcessorImpl;
import network.thunder.core.communication.processor.implementations.gossip.GossipSubject;
import network.thunder.core.communication.processor.implementations.gossip.GossipSubjectImpl;
import network.thunder.core.communication.processor.interfaces.GossipProcessor;
import network.thunder.core.etc.SyncDBHandlerMock;
import network.thunder.core.mesh.Node;
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

    Node node1;
    Node node2;
    Node node3;
    Node node4;

    SyncDBHandlerMock dbHandler1;
    SyncDBHandlerMock dbHandler2;
    SyncDBHandlerMock dbHandler3;
    SyncDBHandlerMock dbHandler4;

    GossipSubject subject1;
    GossipSubject subject2;
    GossipSubject subject3;
    GossipSubject subject4;

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

    @Before
    public void prepare () throws PropertyVetoException, SQLException {

        node1 = new Node();
        node2 = new Node();
        node3 = new Node();
        node4 = new Node();

        node1.isServer = false;
        node2.isServer = true;
        node3.isServer = true;
        node4.isServer = true;
        node1.name = "Gossip1";
        node2.name = "Gossip2";
        node2.name = "Gossip3";
        node2.name = "Gossip4";

        dbHandler1 = new SyncDBHandlerMock();
        dbHandler2 = new SyncDBHandlerMock();
        dbHandler3 = new SyncDBHandlerMock();
        dbHandler4 = new SyncDBHandlerMock();

        subject1 = new GossipSubjectImpl(dbHandler1);
        subject2 = new GossipSubjectImpl(dbHandler2);
        subject3 = new GossipSubjectImpl(dbHandler3);
        subject4 = new GossipSubjectImpl(dbHandler4);

        messageFactory = new GossipMessageFactoryImpl();
        messageFactory = new GossipMessageFactoryImpl();
        messageFactory = new GossipMessageFactoryImpl();
        messageFactory = new GossipMessageFactoryImpl();

        gossipProcessor12 = new GossipProcessorImpl(messageFactory, subject1, dbHandler1, node1);

        gossipProcessor21 = new GossipProcessorImpl(messageFactory, subject2, dbHandler2, node2);
        gossipProcessor23 = new GossipProcessorImpl(messageFactory, subject2, dbHandler2, node2);

        gossipProcessor32 = new GossipProcessorImpl(messageFactory, subject3, dbHandler3, node3);
        gossipProcessor34 = new GossipProcessorImpl(messageFactory, subject3, dbHandler3, node3);

        gossipProcessor43 = new GossipProcessorImpl(messageFactory, subject4, dbHandler4, node4);

        channel12 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor12, "Gossip12"));

        channel21 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor21, "Gossip21"));
        channel23 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor23, "Gossip23"));

        channel32 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor32, "Gossip32"));
        channel34 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor34, "Gossip34"));

        channel43 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor43, "Gossip43"));

        channel12.readOutbound();

        channel21.readOutbound();
        channel23.readOutbound();

        channel32.readOutbound();
        channel34.readOutbound();

        channel43.readOutbound();
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
        for (int i = 0; i < GossipProcessor.OBJECT_AMOUNT_TO_SEND + 1; i++) {
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

        assertTrue(checkListsEqual(dbHandler2.totalList, dataList));
        assertTrue(checkListsEqual(dbHandler3.totalList, dataList));
        assertTrue(checkListsEqual(dbHandler4.totalList, dataList));
    }

    public boolean checkListsEqual (List list1, List list2) {
        return list1.containsAll(list2) && list2.containsAll(list1);
    }

}