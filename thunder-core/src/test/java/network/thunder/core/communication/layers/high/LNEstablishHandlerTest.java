package network.thunder.core.communication.layers.high;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.ConnectionManager;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.ContextFactoryImpl;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.ProcessorHandler;
import network.thunder.core.communication.layer.high.channel.establish.LNEstablishProcessorImpl;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.BroadcastHelper;
import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.inmemory.InMemoryDBHandler;
import network.thunder.core.etc.*;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.blockchain.MockBlockchainHelper;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.wallet.MockWallet;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;

import static org.junit.Assert.*;

//TODO need to rewrite the tests for wrong signatures / parameters after switching to 2-of-2 anchor
public class LNEstablishHandlerTest {

    EmbeddedChannel channel1;
    EmbeddedChannel channel2;

    ServerObject serverObject1 = new ServerObject();
    ServerObject serverObject2 = new ServerObject();

    ClientObject node1 = new ClientObject(serverObject2);
    ClientObject node2 = new ClientObject(serverObject1);

    ContextFactory contextFactory1;
    ContextFactory contextFactory2;

    LNEstablishProcessorImpl processor1;
    LNEstablishProcessorImpl processor2;

    DBHandler dbHandler1 = new InMemoryDBHandler();
    DBHandler dbHandler2 = new InMemoryDBHandler();

    MockBlockchainHelper mockBlockchainHelper = new MockBlockchainHelper();
    MockBroadcastHelper broadcastHelper = new MockBroadcastHelper();

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        node1.isServer = false;
        node1.intent = ConnectionIntent.OPEN_CHANNEL;
        node2.isServer = true;

        contextFactory1 = new EstablishMockContextFactory(serverObject1, dbHandler1);
        contextFactory2 = new EstablishMockContextFactory(serverObject2, dbHandler2);

        processor1 = new LNEstablishProcessorImpl(contextFactory1, dbHandler1, node1);
        processor2 = new LNEstablishProcessorImpl(contextFactory2, dbHandler2, node2);

        channel1 = new EmbeddedChannel(new ProcessorHandler(processor1, "LNEstablish1"));
        channel2 = new EmbeddedChannel(new ProcessorHandler(processor2, "LNEstablish2"));

        contextFactory1.getChannelManager().openChannel(node1.nodeKey, new ChannelOpenListener());

        Message m = (Message) channel2.readOutbound();
        assertNull(m);
    }

    public void after () {
        channel1.checkException();
        channel2.checkException();
    }

    @Test
    public void fullExchangeShouldBroadcast () throws InterruptedException {

        TestTools.exchangeMessagesDuplex(channel1, channel2);
        TestTools.exchangeMessagesDuplex(channel1, channel2);
        TestTools.exchangeMessagesDuplex(channel1, channel2);

        Thread.sleep(500);
        //TODO somehow test inclusion in block as well...
        assertTrue(mockBlockchainHelper.broadcastedTransactionHashes.contains(processor1.establishProgress.channel.anchorTxHash));

        //TODO check all properties of the broadcasted objects..
        assertEquals(4, broadcastHelper.broadcastedObjects.size());

        after();
    }

    @Test
    public void shouldNotAcceptMessageInWrongOrder () {
        Message c1 = (Message) channel1.readOutbound();
        channel2.writeInbound(c1);
        Message c2 = (Message) channel2.readOutbound();
        channel1.writeInbound(c2);
        channel2.writeInbound(c1);
        assertFalse(channel2.isOpen());
        after();
    }

    class EstablishMockContextFactory extends ContextFactoryImpl {
        ConnectionManager connManager;

        public EstablishMockContextFactory (ServerObject node, DBHandler dbHandler) {
            super(node, dbHandler, new MockWallet(Constants.getNetwork()), new MockLNEventHelper());
            connManager = new MockConnectionManager();

        }

        @Override
        public BlockchainHelper getBlockchainHelper () {
            return mockBlockchainHelper;
        }

        @Override
        public BroadcastHelper getBroadcastHelper () {
            return broadcastHelper;
        }

        @Override
        public ConnectionManager getConnectionManager () {
            if (connManager == null) {
                connManager = new MockConnectionManager();
            }
            return connManager;
        }

    }

}