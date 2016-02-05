package network.thunder.core.communication.nio.handler.low;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.impl.MessageEncrypterImpl;
import network.thunder.core.communication.objects.messages.impl.MessageSerializerImpl;
import network.thunder.core.communication.objects.messages.impl.factories.EncryptionMessageFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptedMessage;
import network.thunder.core.communication.objects.messages.impl.message.encryption.EncryptionInitialMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.EncryptionMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageEncrypter;
import network.thunder.core.communication.processor.implementations.EncryptionProcessorImpl;
import network.thunder.core.communication.processor.interfaces.EncryptionProcessor;
import network.thunder.core.etc.RandomDataMessage;
import network.thunder.core.etc.crypto.ECDHKeySet;
import network.thunder.core.mesh.Node;
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
    P2PContext context;
    Node node1;
    Node node2;

    ProcessorHandler handler1;
    ProcessorHandler handler2;

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        context = new P2PContext(8992);

        node1 = new Node();
        node2 = new Node();

        node1.isServer = false;
        node2.isServer = true;

        context.connectedNodes.add(node1);
        context.connectedNodes.add(node2);

        MessageEncrypter messageEncrypter = new MessageEncrypterImpl(new MessageSerializerImpl());

        EncryptionMessageFactory messageFactory = new EncryptionMessageFactoryImpl();

        EncryptionProcessor encryptionProcessor1 = new EncryptionProcessorImpl(messageFactory, messageEncrypter, node1);
        EncryptionProcessor encryptionProcessor2 = new EncryptionProcessorImpl(messageFactory, messageEncrypter, node2);

        handler1 = new ProcessorHandler(encryptionProcessor1, "Encryption1");
        handler2 = new ProcessorHandler(encryptionProcessor2, "Encryption2");

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

    //        byte[] encrypted = CryptoTools.encryptAES_CTR(testMessage.data, node1.ecdhKeySet.encryptionKey, node1.ecdhKeySet.ivServer, 0);
//        byte[] hmac = CryptoTools.getHMAC(encrypted, node1.ecdhKeySet.hmacKey);
//        System.out.println(Tools.bytesToHex(en));
//        System.out.println(Tools.bytesToHex(message1));

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