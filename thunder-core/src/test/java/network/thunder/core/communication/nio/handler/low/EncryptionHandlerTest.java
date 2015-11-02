package network.thunder.core.communication.nio.handler.low;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.database.DatabaseHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.etc.crypto.CryptoTools;
import network.thunder.core.mesh.Node;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

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

    EncryptionHandler handler;

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        context = new P2PContext(8992);

        node1 = new Node();
        node1.conn = DatabaseHandler.getDataSource().getConnection();

        node2 = new Node();
        node2.conn = DatabaseHandler.getDataSource().getConnection();

        context.connectedNodes.add(node1);
        context.connectedNodes.add(node2);

        handler = new EncryptionHandler(false, node1);
        channel1 = new EmbeddedChannel(handler);
        channel2 = new EmbeddedChannel(new EncryptionHandler(true, node2));

        m = (Message) channel2.readOutbound();
        assertNull(m);

    }

    @Test
    public void clientShouldSendPublicKey () throws Exception {

        ByteBuf buffer = (ByteBuf) channel1.readOutbound();
        byte[] key = new byte[node1.getPubKeyTempServer().getPubKey().length];
        buffer.readBytes(key);
        assertTrue(Arrays.equals(key, node1.getPubKeyTempServer().getPubKey()));

    }

    @Test
    public void serverShouldRespondPublicKey () throws Exception {

        ByteBuf buffer = (ByteBuf) channel1.readOutbound();
        byte[] key = new byte[node1.getPubKeyTempServer().getPubKey().length];
        buffer.readBytes(key);
        assertTrue(Arrays.equals(key, node1.getPubKeyTempServer().getPubKey()));

        buffer.resetReaderIndex();

        channel2.writeInbound(buffer);
        buffer = (ByteBuf) channel2.readOutbound();
        key = new byte[node1.getPubKeyTempServer().getPubKey().length];
        buffer.readBytes(key);
        assertTrue(Arrays.equals(key, node2.getPubKeyTempServer().getPubKey()));

    }

    @Test
    public void shouldEncryptAndDecrypt () {
        //Give each nodes the key of the other node..
        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());

        //Create some gibberish to parse through them
        byte[] message = new byte[1024];
        Random r = new Random();
        r.nextBytes(message);

        ByteBuf messageB = node1.getNettyContext().alloc().buffer();
        messageB.writeBytes(message);

        channel1.writeOutbound(messageB);
        ByteBuf buffer = (ByteBuf) channel1.readOutbound();
        byte[] message1 = new byte[buffer.readableBytes()];
        buffer.readBytes(message1);
        buffer.resetReaderIndex();
        assertFalse(Arrays.equals(message, message1));

        channel2.writeInbound(buffer);
        buffer = (ByteBuf) channel2.readInbound();
        message1 = new byte[buffer.readableBytes()];
        buffer.readBytes(message1);
        buffer.resetReaderIndex();
        assertTrue(Arrays.equals(message, message1));

        //Test other direction
        message = new byte[1024];
        r = new Random();
        r.nextBytes(message);

        messageB = node1.getNettyContext().alloc().buffer();
        messageB.writeBytes(message);

        channel2.writeOutbound(messageB);
        buffer = (ByteBuf) channel2.readOutbound();

        message1 = new byte[buffer.readableBytes()];
        buffer.readBytes(message1);
        buffer.resetReaderIndex();

        System.out.println(Tools.bytesToHex(message));
        System.out.println(Tools.bytesToHex(message1));

        assertFalse(Arrays.equals(message, message1));

        channel1.writeInbound(buffer);
        buffer = (ByteBuf) channel1.readInbound();
        message1 = new byte[buffer.readableBytes()];
        buffer.readBytes(message1);
        buffer.resetReaderIndex();
        assertTrue(Arrays.equals(message, message1));

        System.out.println(Tools.bytesToHex(message));
        System.out.println(Tools.bytesToHex(message1));

    }

    @Test
    public void shouldEncrypt () throws InvalidKeyException, NoSuchAlgorithmException {
        //Give each nodes the key of the other node..
        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());

        //Create some gibberish to parse through them
        byte[] message = new byte[1024];
        Random r = new Random();
        r.nextBytes(message);

        ByteBuf messageB = node1.getNettyContext().alloc().buffer();
        messageB.writeBytes(message);

        channel1.writeOutbound(messageB);
        ByteBuf buffer = (ByteBuf) channel1.readOutbound();
        byte[] message1 = new byte[buffer.readableBytes()];
        buffer.readBytes(message1);

        //Check whether message1 is indeed correctly encrypted and constructed
        byte[] encrypted = CryptoTools.encryptAES_CTR(message, handler.ecdhKeySet.getEncryptionKey(), handler.ecdhKeySet.getIvServer(), 0);
        byte[] messageSelfEncrypted = CryptoTools.addHMAC(encrypted, handler.ecdhKeySet.getHmacKey());

        System.out.println(Tools.bytesToHex(messageSelfEncrypted));
        System.out.println(Tools.bytesToHex(message1));

        assertTrue(Arrays.equals(messageSelfEncrypted, message1));
    }

}