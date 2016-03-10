package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedGetMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.communication.processor.interfaces.PeerSeedProcessor;
import network.thunder.core.etc.MockContextFactory;
import network.thunder.core.etc.SeedDBHandlerMock;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class PeerSeedHandlerTest {

    EmbeddedChannel channel1;
    EmbeddedChannel channel2;

    NodeServer nodeServer1 = new NodeServer();
    NodeServer nodeServer2 = new NodeServer();

    NodeClient node1 = new NodeClient(nodeServer2);
    NodeClient node2 = new NodeClient(nodeServer1);

    ContextFactory contextFactory1;
    ContextFactory contextFactory2;

    SeedDBHandlerMock dbHandler1 = new SeedDBHandlerMock();
    SeedDBHandlerMock dbHandler2 = new SeedDBHandlerMock();

    @Before
    public void prepare () {
        node1.isServer = false;
        node1.intent = ChannelIntent.GET_IPS;

        node2.isServer = true;

        contextFactory1 = new MockContextFactory(nodeServer1, dbHandler1);
        contextFactory2 = new MockContextFactory(nodeServer2, dbHandler2);

        dbHandler2.fillWithRandomData();

        channel1 = new EmbeddedChannel(new ProcessorHandler(contextFactory1.getPeerSeedProcessor(node1), "Seed1"));
        channel2 = new EmbeddedChannel(new ProcessorHandler(contextFactory2.getPeerSeedProcessor(node2), "Seed2"));

        Message m = (Message) channel2.readOutbound();
        assertNull(m);
    }

    public void after () {
        channel1.checkException();
        channel2.checkException();
    }

    @Test
    public void testSyncingIPObjects () {

        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());

        assertEquals(PeerSeedProcessor.PEERS_TO_SEND, dbHandler1.pubkeyIPObjectArrayList.size());
        assertTrue(dbHandler2.pubkeyIPObjectArrayList.containsAll(dbHandler1.pubkeyIPObjectArrayList));
        after();
    }

    @Test
    public void shouldAskForIPs () {
        Message message = (Message) channel1.readOutbound();
        assertThat(message, instanceOf(PeerSeedGetMessage.class));
    }

}