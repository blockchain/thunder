package network.thunder.core.communication.nio.handler.mid;

import com.google.gson.Gson;
import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.Type;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.objects.subobjects.AuthenticationObject;
import network.thunder.core.database.DatabaseHandler;
import network.thunder.core.mesh.Node;
import org.bitcoinj.core.ECKey;
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
    Message m;
    P2PContext context;
    Node node1;
    Node node2;

    ECKey key1 = new ECKey();
    ECKey key2 = new ECKey();
    ECKey tempKey1 = new ECKey();
    ECKey tempKey2 = new ECKey();

//    EncryptionHandler handler;

    @Test
    public void authenticationFail () throws NoSuchProviderException, NoSuchAlgorithmException {
        m = (Message) channel1.readOutbound();
        assertEquals(m.type, Type.AUTH_SEND);
        AuthenticationObject authObject = new Gson().fromJson(m.data, AuthenticationObject.class);
        assertTrue(node2.processAuthentication(authObject, ECKey.fromPublicOnly(authObject.pubkeyServer), tempKey2));

        byte[] b = new byte[4];
        Random r = new Random();
        r.nextBytes(b);
        System.arraycopy(b, 0, authObject.signature, 10, 4);

        channel2.writeInbound(new Message(authObject, Type.AUTH_SEND));

        m = (Message) channel2.readOutbound();
        assertEquals(m.type, Type.AUTH_FAILED);

    }

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        context = new P2PContext(8992);

        node1 = new Node();
        node1.setPubKeyTempServer(tempKey1);
        node1.setPubKeyTempClient(tempKey2);
        node1.conn = DatabaseHandler.getDataSource().getConnection();

        node2 = new Node();
        node2.setPubKeyTempServer(tempKey2);
        node2.setPubKeyTempClient(tempKey1);
        node2.conn = DatabaseHandler.getDataSource().getConnection();

        context.connectedNodes.add(node1);
        context.connectedNodes.add(node2);

//        handler = new EncryptionHandler(false, node1);
        channel1 = new EmbeddedChannel(new AuthenticationHandler(key1, false, node1));
        channel2 = new EmbeddedChannel(new AuthenticationHandler(key2, true, node2));

        m = (Message) channel2.readOutbound();
        assertNull(m);

    }

    @Test
    public void testAuthenticationHandshake () throws NoSuchProviderException, NoSuchAlgorithmException {
        m = (Message) channel1.readOutbound();
        assertEquals(m.type, Type.AUTH_SEND);
        AuthenticationObject authObject = new Gson().fromJson(m.data, AuthenticationObject.class);
        assertTrue(node2.processAuthentication(authObject, ECKey.fromPublicOnly(authObject.pubkeyServer), tempKey2));

        channel2.writeInbound(m);
        m = (Message) channel2.readOutbound();
        assertEquals(m.type, Type.AUTH_SEND);
        authObject = new Gson().fromJson(m.data, AuthenticationObject.class);
        assertTrue(node2.processAuthentication(authObject, ECKey.fromPublicOnly(authObject.pubkeyServer), tempKey1));

    }

}