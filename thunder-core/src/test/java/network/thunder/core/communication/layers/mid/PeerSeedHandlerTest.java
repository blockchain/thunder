package network.thunder.core.communication.layers.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.ProcessorHandler;
import network.thunder.core.communication.layer.middle.peerseed.PeerSeedProcessor;
import network.thunder.core.communication.layer.middle.peerseed.messages.PeerSeedGetMessage;
import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.etc.MockContextFactory;
import network.thunder.core.etc.SeedDBHandlerMock;
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

    ServerObject serverObject1 = new ServerObject();
    ServerObject serverObject2 = new ServerObject();

    ClientObject node1 = new ClientObject(serverObject2);
    ClientObject node2 = new ClientObject(serverObject1);

    ContextFactory contextFactory1;
    ContextFactory contextFactory2;

    SeedDBHandlerMock dbHandler1 = new SeedDBHandlerMock();
    SeedDBHandlerMock dbHandler2 = new SeedDBHandlerMock();

    @Before
    public void prepare () {
        node1.isServer = false;
        node1.intent = ConnectionIntent.GET_IPS;

        node2.isServer = true;

        contextFactory1 = new MockContextFactory(serverObject1, dbHandler1);
        contextFactory2 = new MockContextFactory(serverObject2, dbHandler2);

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