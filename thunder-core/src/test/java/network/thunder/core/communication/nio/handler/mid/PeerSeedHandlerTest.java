package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.impl.factories.PeerSeedMessageFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.peerseed.PeerSeedGetMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.PeerSeedMessageFactory;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.communication.processor.implementations.PeerSeedProcessorImpl;
import network.thunder.core.communication.processor.interfaces.PeerSeedProcessor;
import network.thunder.core.etc.SeedDBHandlerMock;
import network.thunder.core.mesh.Node;
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

    Node node1;
    Node node2;

    PeerSeedMessageFactory messageFactory;

    PeerSeedProcessor seedProcessor1;
    PeerSeedProcessor seedProcessor2;

    SeedDBHandlerMock handler1;
    SeedDBHandlerMock handler2;

    @Before
    public void prepare () {
        node1 = new Node();
        node2 = new Node();

        node1.isServer = false;
        node1.intent = ChannelIntent.GET_IPS;

        node2.isServer = true;

        messageFactory = new PeerSeedMessageFactoryImpl();

        handler1 = new SeedDBHandlerMock();
        handler2 = new SeedDBHandlerMock();
        handler2.fillWithRandomData();

        seedProcessor1 = new PeerSeedProcessorImpl(messageFactory, handler1, node1);
        seedProcessor2 = new PeerSeedProcessorImpl(messageFactory, handler2, node2);

        channel1 = new EmbeddedChannel(new ProcessorHandler(seedProcessor1, "Seed1"));
        channel2 = new EmbeddedChannel(new ProcessorHandler(seedProcessor2, "Seed2"));

        Message m = (Message) channel2.readOutbound();
        assertNull(m);
    }

    @Test
    public void testSyncingIPObjects () {

        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());

        assertEquals(PeerSeedProcessor.PEERS_TO_SEND, handler1.pubkeyIPObjectArrayList.size());
        assertTrue(handler2.pubkeyIPObjectArrayList.containsAll(handler1.pubkeyIPObjectArrayList));
    }

    @Test
    public void shouldAskForIPs () {
        Message message = (Message) channel1.readOutbound();
        assertThat(message, instanceOf(PeerSeedGetMessage.class));
    }

}