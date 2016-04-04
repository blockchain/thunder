package network.thunder.core.communication.layers.high;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.ContextFactoryImpl;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.ProcessorHandler;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.channel.close.LNCloseException;
import network.thunder.core.communication.layer.high.channel.close.LNCloseProcessorImpl;
import network.thunder.core.communication.layer.high.channel.close.messages.LNCloseAMessage;
import network.thunder.core.communication.layer.middle.broadcasting.gossip.BroadcastHelper;
import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.InMemoryDBHandler;
import network.thunder.core.etc.*;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import network.thunder.core.helper.blockchain.MockBlockchainHelper;
import network.thunder.core.helper.callback.results.NullResultCommand;
import network.thunder.core.helper.wallet.MockWallet;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by matsjerratsch on 12/11/15.
 */
public class LNCloseHandlerTest {

    EmbeddedChannel embeddedChannel1;
    EmbeddedChannel embeddedChannel2;

    Channel channel1;
    Channel channel2;

    ServerObject serverObject1 = new ServerObject();
    ServerObject serverObject2 = new ServerObject();

    ClientObject node1 = new ClientObject(serverObject2);
    ClientObject node2 = new ClientObject(serverObject1);

    ContextFactory contextFactory1;
    ContextFactory contextFactory2;

    LNCloseProcessorImpl processor1;
    LNCloseProcessorImpl processor2;

    DBHandler dbHandler1 = new InMemoryDBHandler();
    DBHandler dbHandler2 = new InMemoryDBHandler();

    MockBlockchainHelper mockBlockchainHelper = new MockBlockchainHelper();
    MockBroadcastHelper broadcastHelper = new MockBroadcastHelper();

    MockChannelManager channelManager = new MockChannelManager();

    LNConfiguration configuration = new LNConfiguration();

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        node1.isServer = false;
        node1.intent = ConnectionIntent.OPEN_CHANNEL;
        node2.isServer = true;

        contextFactory1 = new EstablishMockContextFactory(serverObject1, dbHandler1);
        contextFactory2 = new EstablishMockContextFactory(serverObject2, dbHandler2);

        processor1 = new LNCloseProcessorImpl(contextFactory1, dbHandler1, node1);
        processor2 = new LNCloseProcessorImpl(contextFactory2, dbHandler2, node2);

        channel1 = new Channel();
        channel2 = new Channel();

        channel1.nodeId = node1.pubKeyClient.getPubKey();
        channel2.nodeId = node2.pubKeyClient.getPubKey();

        channel1.retrieveDataFromOtherChannel(channel2);
        channel2.retrieveDataFromOtherChannel(channel1);

        dbHandler1.saveChannel(channel1);
        dbHandler2.saveChannel(channel2);

        embeddedChannel1 = new EmbeddedChannel(new ProcessorHandler(processor1, "LNClose1"));
        embeddedChannel2 = new EmbeddedChannel(new ProcessorHandler(processor2, "LNClose2"));

        Message m = (Message) embeddedChannel2.readOutbound();
        assertNull(m);
        m = (Message) embeddedChannel1.readOutbound();
        assertNull(m);
    }

    public void after () {
        embeddedChannel1.checkException();
        embeddedChannel2.checkException();
    }

    @Test
    public void shouldSendMessage () {
        channelManager.closeChannel(channel1, new NullResultCommand());
        LNCloseAMessage message = (LNCloseAMessage) embeddedChannel1.readOutbound();
        assertNotNull(message);
        after();
    }

    @Test
    public void shouldVerifyMessage () {
        channelManager.closeChannel(channel1, new NullResultCommand());
        LNCloseAMessage message = (LNCloseAMessage) embeddedChannel1.readOutbound();
        embeddedChannel2.writeInbound(message);
        after();
    }

    @Test(expected = LNCloseException.class)
    public void shouldFailSignatureOne () {
        channelManager.closeChannel(channel1, new NullResultCommand());
        LNCloseAMessage message = (LNCloseAMessage) embeddedChannel1.readOutbound();
        Tools.copyRandomByteInByteArray(message.signatureList.get(0), 30, 2);
        embeddedChannel2.writeInbound(message);
        after();
    }

    @Test(expected = LNCloseException.class)
    public void shouldFailSignatureTwo () {
        channelManager.closeChannel(channel1, new NullResultCommand());
        LNCloseAMessage message = (LNCloseAMessage) embeddedChannel1.readOutbound();
        Tools.copyRandomByteInByteArray(message.signatureList.get(1), 30, 2);
        embeddedChannel2.writeInbound(message);
        after();
    }

    class EstablishMockContextFactory extends ContextFactoryImpl {

        public EstablishMockContextFactory (ServerObject node, DBHandler dbHandler) {
            super(node, dbHandler, new MockWallet(Constants.getNetwork()), new MockLNEventHelper());
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
        public ChannelManager getChannelManager () {
            return channelManager;
        }
    }

}