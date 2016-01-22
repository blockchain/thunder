package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.impl.factories.GossipMessageFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.gossip.GossipInvMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.GossipMessageFactory;
import network.thunder.core.communication.processor.implementations.gossip.GossipProcessorImpl;
import network.thunder.core.communication.processor.implementations.gossip.GossipSubject;
import network.thunder.core.communication.processor.implementations.gossip.GossipSubjectImpl;
import network.thunder.core.communication.processor.interfaces.GossipProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.DBHandlerMock;
import network.thunder.core.mesh.Node;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by matsjerratsch on 27/10/2015.
 */
public class GossipHandlerTest {

    Node node1;
    Node node2;
    Node node3;
    Node node4;

    DBHandler dbHandler1;
    DBHandler dbHandler2;
    DBHandler dbHandler3;
    DBHandler dbHandler4;

    GossipSubject subject1;
    GossipSubject subject2;
    GossipSubject subject3;
    GossipSubject subject4;

    GossipMessageFactory messageFactory1;
    GossipMessageFactory messageFactory2;
    GossipMessageFactory messageFactory3;
    GossipMessageFactory messageFactory4;

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

        subject1 = new GossipSubjectImpl();
        subject2 = new GossipSubjectImpl();
        subject3 = new GossipSubjectImpl();
        subject4 = new GossipSubjectImpl();

        dbHandler1 = new DBHandlerMock();
        dbHandler2 = new DBHandlerMock();
        dbHandler3 = new DBHandlerMock();
        dbHandler4 = new DBHandlerMock();

        messageFactory1 = new GossipMessageFactoryImpl();
        messageFactory2 = new GossipMessageFactoryImpl();
        messageFactory3 = new GossipMessageFactoryImpl();
        messageFactory4 = new GossipMessageFactoryImpl();

        gossipProcessor12 = new GossipProcessorImpl(messageFactory1, subject1, dbHandler1, node1);

        gossipProcessor21 = new GossipProcessorImpl(messageFactory2, subject2, dbHandler2, node2);
        gossipProcessor23 = new GossipProcessorImpl(messageFactory2, subject2, dbHandler2, node2);

        gossipProcessor32 = new GossipProcessorImpl(messageFactory3, subject3, dbHandler3, node3);
        gossipProcessor34 = new GossipProcessorImpl(messageFactory3, subject3, dbHandler3, node3);

        gossipProcessor43 = new GossipProcessorImpl(messageFactory4, subject4, dbHandler4, node4);

        channel12 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor12, "Gossip12"));

        channel21 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor21, "Gossip21"));
        channel23 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor23, "Gossip23"));

        channel32 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor32, "Gossip32"));
        channel34 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor34, "Gossip34"));

        channel43 = new EmbeddedChannel(new ProcessorHandler(gossipProcessor43, "Gossip43"));
    }

    @Test
    public void shouldAskForDataAfterInv () throws Exception {
        channel12.readOutbound();
        channel21.readOutbound();

        ArrayList<byte[]> invList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            byte[] b = new byte[20];
            new Random().nextBytes(b);
            invList.add(b);
        }

        GossipInvMessage message = messageFactory1.getGossipInvMessage(invList);

        channel12.writeInbound(message);

        Message message1 = (Message) channel12.readOutbound();
        assertThat(message1, instanceOf(GossipGetMessage.class));

        GossipGetMessage getMessage = (GossipGetMessage) message1;

        for (int i = 0; i < invList.size(); i++) {
            assertTrue(Arrays.equals(getMessage.inventoryList.get(i), invList.get(i)));
        }

        channel12.checkException();
    }

}