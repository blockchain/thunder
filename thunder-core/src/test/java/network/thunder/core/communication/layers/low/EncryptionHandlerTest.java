package network.thunder.core.communication.layers.low;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.ProcessorHandler;
import network.thunder.core.communication.layer.low.encryption.MessageEncrypterImpl;
import network.thunder.core.communication.layer.low.serialisation.MessageSerializerImpl;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptedMessage;
import network.thunder.core.communication.layer.low.encryption.messages.EncryptionInitialMessage;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.low.encryption.MessageEncrypter;
import network.thunder.core.etc.MockContextFactory;
import network.thunder.core.etc.RandomDataMessage;
import network.thunder.core.helper.crypto.ECDHKeySet;
import network.thunder.core.communication.ServerObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by matsjerratsch on 29/10/2015.
 */
public class EncryptionHandlerTest {

    EmbeddedChannel channel1;
    EmbeddedChannel channel2;
    Message m;
    ClientObject node1 = new ClientObject();
    ClientObject node2 = new ClientObject();

    ServerObject serverObject1 = new ServerObject();
    ServerObject serverObject2 = new ServerObject();

    ProcessorHandler handler1;
    ProcessorHandler handler2;

    ContextFactory contextFactory1 = new MockContextFactory(serverObject1);
    ContextFactory contextFactory2 = new MockContextFactory(serverObject2);

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        node1.isServer = false;
        node2.isServer = true;

        handler1 = new ProcessorHandler(contextFactory1.getEncryptionProcessor(node1), "Encryption1");
        handler2 = new ProcessorHandler(contextFactory2.getEncryptionProcessor(node2), "Encryption2");

        channel1 = new EmbeddedChannel(handler1);
        channel2 = new EmbeddedChannel(handler2);

        m = (Message) channel2.readOutbound();
        assertNull(m);
    }

    @After
    public void after () {
        channel1.checkException();
        channel2.checkException();
    }

    @Test
    public void clientShouldSendPublicKey () throws Exception {
        EncryptionInitialMessage EncryptionInitialMessage1 = (EncryptionInitialMessage) channel1.readOutbound();
        assertTrue(Arrays.equals(EncryptionInitialMessage1.key, node1.ephemeralKeyServer.getPubKey()));
    }

    @Test
    public void serverShouldRespondPublicKey () throws Exception {
        channel2.writeInbound(channel1.readOutbound());
        EncryptionInitialMessage EncryptionInitialMessage2 = (EncryptionInitialMessage) channel2.readOutbound();
        assertTrue(Arrays.equals(EncryptionInitialMessage2.key, node2.ephemeralKeyServer.getPubKey()));
    }

    @Test
    public void shouldEncrypt () throws InvalidKeyException, NoSuchAlgorithmException {
        doHandShake();

        ECDHKeySet keySetBeforeEncryption1 = node1.ecdhKeySet.clone();

        RandomDataMessage testMessage = new RandomDataMessage();
        channel1.writeOutbound(testMessage);

        EncryptedMessage encryptedMessage = (EncryptedMessage) channel1.readOutbound();
        MessageEncrypter encrypter = new MessageEncrypterImpl(new MessageSerializerImpl());

        EncryptedMessage messageSelfEncrypted = encrypter.encrypt(testMessage, keySetBeforeEncryption1);
        assertTrue(Arrays.equals(messageSelfEncrypted.payload, encryptedMessage.payload));
        assertTrue(Arrays.equals(encryptedMessage.hmac, encryptedMessage.hmac));
    }

    @Test
    public void shouldEncryptAndDecrypt () {
        doHandShake();

        RandomDataMessage testMessage = new RandomDataMessage();
        channel1.writeOutbound(testMessage);

        //Test other direction
        channel2.writeInbound(channel1.readOutbound());
        RandomDataMessage decryptedMessage = (RandomDataMessage) channel2.readInbound();

        assertEquals(decryptedMessage, testMessage);
    }

    public void doHandShake () {
        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());
    }

}