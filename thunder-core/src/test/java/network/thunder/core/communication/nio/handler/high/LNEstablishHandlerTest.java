package network.thunder.core.communication.nio.handler.high;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.impl.blockchainlistener.MockBlockchainHelper;
import network.thunder.core.communication.objects.messages.impl.factories.ContextFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lightningestablish.LNEstablishDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.BlockchainHelper;
import network.thunder.core.communication.objects.messages.interfaces.message.FailureMessage;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.communication.processor.exceptions.LNEstablishException;
import network.thunder.core.communication.processor.implementations.LNEstablishProcessorImpl;
import network.thunder.core.communication.processor.implementations.gossip.BroadcastHelper;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.etc.*;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by matsjerratsch on 12/11/15.
 */
public class LNEstablishHandlerTest {

    EmbeddedChannel channel1;
    EmbeddedChannel channel2;

    NodeServer nodeServer1 = new NodeServer();
    NodeServer nodeServer2 = new NodeServer();

    NodeClient node1 = new NodeClient(nodeServer2);
    NodeClient node2 = new NodeClient(nodeServer1);

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
        node1.intent = ChannelIntent.OPEN_CHANNEL;
        node2.isServer = true;

        contextFactory1 = new EstablishMockContextFactory(nodeServer1, dbHandler1);
        contextFactory2 = new EstablishMockContextFactory(nodeServer2, dbHandler2);

        processor1 = new LNEstablishProcessorImpl(contextFactory1, dbHandler1, node1);
        processor2 = new LNEstablishProcessorImpl(contextFactory2, dbHandler2, node2);

        channel1 = new EmbeddedChannel(new ProcessorHandler(processor1, "LNEstablish1"));
        channel2 = new EmbeddedChannel(new ProcessorHandler(processor2, "LNEstablish2"));

        Message m = (Message) channel2.readOutbound();
        assertNull(m);
    }

    public void after () {
        channel1.checkException();
        channel2.checkException();
    }

    @Test
    public void shouldSendMessageA () {

        LNEstablishAMessage message = (LNEstablishAMessage) channel1.readOutbound();

        Channel channel = processor1.channel;

        //Should send keys, secretHash and amounts
        assertTrue(Arrays.equals(message.pubKeyEscape, channel.getKeyServer().getPubKey()));
        assertTrue(Arrays.equals(message.pubKeyFastEscape, channel.getKeyServerA().getPubKey()));
        assertTrue(Arrays.equals(message.secretHashFastEscape, channel.getAnchorSecretHashServer()));
        assertEquals(message.clientAmount, channel.getAmountClient());
        assertEquals(message.serverAmount, channel.getAmountServer());

        after();
    }

    @Test
    public void shouldSendMessageB () {

        channel2.writeInbound(channel1.readOutbound());

        LNEstablishBMessage message = (LNEstablishBMessage) channel2.readOutbound();

        Channel channel = processor2.channel;

        //Should send keys, secretHash and amounts
        assertTrue(Arrays.equals(message.pubKeyEscape, channel.getKeyServer().getPubKey()));
        assertTrue(Arrays.equals(message.pubKeyFastEscape, channel.getKeyServerA().getPubKey()));
        assertTrue(Arrays.equals(message.secretHashFastEscape, channel.getAnchorSecretHashServer()));
        assertTrue(Arrays.equals(message.anchorHash, channel.getAnchorTransactionServer(contextFactory1.getWalletHelper()).getHash().getBytes()));
        assertEquals(message.serverAmount, channel.getAmountServer());
        after();
    }

    @Test
    public void shouldSendMessageC () {
        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());

        LNEstablishCMessage message = (LNEstablishCMessage) channel1.readOutbound();

        Channel channel1 = processor1.channel;
        Channel channel2 = processor2.channel;

        //Should send keys, secretHash and amounts
        assertTrue(Arrays.equals(message.anchorHash, channel1.getAnchorTransactionServer(contextFactory2.getWalletHelper()).getHash().getBytes()));

        Transaction escape = channel2.getEscapeTransactionServer();
        Transaction fastEscape = channel2.getFastEscapeTransactionServer();

        Sha256Hash hashEscape = escape.hashForSignature(0, channel2.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
        Sha256Hash hashFastEscape = fastEscape.hashForSignature(0, channel2.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);

        assertTrue(channel2.getKeyClientA().verify(hashEscape, TransactionSignature.decodeFromBitcoin(message.signatureEscape, true)));
        assertTrue(channel2.getKeyClientA().verify(hashFastEscape, TransactionSignature.decodeFromBitcoin(message.signatureFastEscape, true)));
        after();
    }

    @Test
    public void shouldSendMessageD () {
        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());
        channel2.writeInbound(channel1.readOutbound());

        Channel channel = processor1.channel;

        //Should send keys, secretHash and amounts
        LNEstablishDMessage message = (LNEstablishDMessage) channel2.readOutbound();

        Transaction escape = channel.getEscapeTransactionServer();
        Transaction fastEscape = channel.getFastEscapeTransactionServer();

        Sha256Hash hashEscape = escape.hashForSignature(0, channel.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
        Sha256Hash hashFastEscape = fastEscape.hashForSignature(0, channel.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);

        assertTrue(channel.getKeyClientA().verify(hashEscape, TransactionSignature.decodeFromBitcoin(message.signatureEscape, true)));
        assertTrue(channel.getKeyClientA().verify(hashFastEscape, TransactionSignature.decodeFromBitcoin(message.signatureFastEscape, true)));

        channel1.writeInbound(message);
        after();
    }

    @Test
    public void fullExchangeShouldBroadcast () {
        LNEstablishAMessage aMessage = (LNEstablishAMessage) channel1.readOutbound();
        channel2.writeInbound(aMessage);

        LNEstablishBMessage bMessage = (LNEstablishBMessage) channel2.readOutbound();
        channel1.writeInbound(bMessage);

        LNEstablishCMessage cMessage = (LNEstablishCMessage) channel1.readOutbound();
        channel2.writeInbound(cMessage);

        channel1.writeInbound(channel2.readOutbound());

        //TODO somehow test inclusion in block as well...
        assertTrue(mockBlockchainHelper.broadcastedTransaction.contains(Sha256Hash.wrap(bMessage.anchorHash)));
        assertTrue(mockBlockchainHelper.broadcastedTransaction.contains(Sha256Hash.wrap(cMessage.anchorHash)));

        //TODO check all properties of the broadcasted objects..
        assertTrue(broadcastHelper.broadcastedObjects.size() == 4);

        after();
    }

    @Test(expected = LNEstablishException.class)
    public void shouldNotAcceptSignature () {
        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());

        LNEstablishCMessage message = (LNEstablishCMessage) channel1.readOutbound();
        LNEstablishCMessage messageSwappedSign = new LNEstablishCMessage(message.signatureFastEscape, message.signatureEscape, message.anchorHash);

        channel2.writeInbound(messageSwappedSign);
        assertTrue(channel2.readOutbound() instanceof FailureMessage);
        after();
    }

    @Test(expected = LNEstablishException.class)
    public void shouldNotAcceptMessageInWrongOrder () {
        Message c1 = (Message) channel1.readOutbound();
        channel2.writeInbound(c1);
        Message c2 = (Message) channel2.readOutbound();
        channel1.writeInbound(c2);
        channel2.writeInbound(c1);
        assertTrue(channel2.readOutbound() instanceof FailureMessage);
        after();
    }

    class EstablishMockContextFactory extends ContextFactoryImpl {

        public EstablishMockContextFactory (NodeServer node, DBHandler dbHandler) {
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
    }

}