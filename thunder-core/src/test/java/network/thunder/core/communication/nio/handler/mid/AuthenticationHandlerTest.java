package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.impl.message.authentication.AuthenticationMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.message.FailureMessage;
import network.thunder.core.etc.MockContextFactory;
import network.thunder.core.etc.MockLNEventHelper;
import network.thunder.core.etc.RandomDataMessage;
import network.thunder.core.etc.crypto.ECDH;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.sql.SQLException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class AuthenticationHandlerTest {

    EmbeddedChannel channel1;
    EmbeddedChannel channel2;

    NodeServer nodeServer1 = new NodeServer();
    NodeServer nodeServer2 = new NodeServer();

    NodeClient node1 = new NodeClient(nodeServer2);
    NodeClient node2 = new NodeClient(nodeServer1);

    ContextFactory contextFactory1;
    ContextFactory contextFactory2;

    LNEventHelper eventHelper = new MockLNEventHelper();

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        node1.isServer = false;
        node2.isServer = true;

        contextFactory1 = new MockContextFactory(nodeServer1);
        contextFactory2 = new MockContextFactory(nodeServer2);

        node1.ephemeralKeyClient = node2.ephemeralKeyServer;
        node2.ephemeralKeyClient = node1.ephemeralKeyServer;

        node1.ecdhKeySet = ECDH.getSharedSecret(node1.ephemeralKeyServer, node1.ephemeralKeyClient);
        node2.ecdhKeySet = ECDH.getSharedSecret(node2.ephemeralKeyServer, node2.ephemeralKeyClient);

        channel1 = new EmbeddedChannel(new ProcessorHandler(contextFactory1.getAuthenticationProcessor(node1), "Encryption1"));
        channel2 = new EmbeddedChannel(new ProcessorHandler(contextFactory2.getAuthenticationProcessor(node2), "Encryption2"));

        Message m = (Message) channel2.readOutbound();
        assertNull(m);

    }

    public void after () {
        channel1.checkException();
        channel2.checkException();
    }

    @Test
    public void authenticationFail () throws NoSuchProviderException, NoSuchAlgorithmException {
        AuthenticationMessage authenticationMessage = (AuthenticationMessage) channel1.readOutbound();

        byte[] sig = authenticationMessage.signature;
        byte[] b = new byte[4];
        Random r = new Random();
        r.nextBytes(b);
        System.arraycopy(b, 0, sig, 10, 4);

        AuthenticationMessage falseMessage = new AuthenticationMessage(authenticationMessage.pubKeyServer, sig);

        channel2.writeInbound(falseMessage);

        Message failureMessage = (Message) channel2.readOutbound();
        assertTrue(failureMessage instanceof FailureMessage);
        assertEquals(((FailureMessage) failureMessage).getFailure(), "Signature does not match..");

        after();
    }

    @Test
    public void testAuthenticationHandshake () throws NoSuchProviderException, NoSuchAlgorithmException {
        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());

        assertNull(channel1.readOutbound());
        assertNull(channel2.readOutbound());

        RandomDataMessage randomDataMessage1 = new RandomDataMessage();
        RandomDataMessage randomDataMessage2 = new RandomDataMessage();

        //Should allow both directions now..
        channel1.writeInbound(randomDataMessage1);
        channel2.writeInbound(randomDataMessage2);

        assertEquals(channel1.readInbound(), randomDataMessage1);
        assertEquals(channel2.readInbound(), randomDataMessage2);

        channel1.writeOutbound(randomDataMessage1);
        channel2.writeOutbound(randomDataMessage2);

        assertEquals(channel1.readOutbound(), randomDataMessage1);
        assertEquals(channel2.readOutbound(), randomDataMessage2);

        after();
    }

    @Test
    public void shouldNotAllowMessagePassthrough () throws NoSuchProviderException, NoSuchAlgorithmException {
        channel1.readOutbound();

        assertNull(channel1.readOutbound());
        assertNull(channel2.readOutbound());

        RandomDataMessage randomDataMessage1 = new RandomDataMessage();
        RandomDataMessage randomDataMessage2 = new RandomDataMessage();

        //Should allow both directions now..
        channel1.writeInbound(randomDataMessage1);
        channel2.writeInbound(randomDataMessage2);

        assertNull(channel1.readInbound());
        assertNull(channel2.readInbound());

        channel1.writeOutbound(randomDataMessage1);
        channel2.writeOutbound(randomDataMessage2);

        assertTrue(channel1.readOutbound() instanceof FailureMessage);
        assertTrue(channel2.readOutbound() instanceof FailureMessage);

        after();
    }

}