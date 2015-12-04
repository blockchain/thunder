//package network.thunder.core.communication.nio.handler.high;
//
//import com.google.gson.Gson;
//import io.netty.channel.embedded.EmbeddedChannel;
//import network.thunder.core.communication.Message;
//import network.thunder.core.communication.Type;
//import network.thunder.core.communication.nio.P2PContext;
//import network.thunder.core.communication.objects.lightning.establish.EstablishChannelMessageA;
//import network.thunder.core.communication.objects.lightning.establish.EstablishChannelMessageB;
//import network.thunder.core.communication.objects.lightning.establish.EstablishChannelMessageC;
//import network.thunder.core.communication.objects.lightning.establish.EstablishChannelMessageD;
//import network.thunder.core.communication.objects.p2p.sync.ChannelStatusObject;
//import network.thunder.core.communication.objects.p2p.sync.PubkeyChannelObject;
//import network.thunder.core.communication.objects.p2p.sync.PubkeyIPObject;
//import network.thunder.core.database.objects.Channel;
//import network.thunder.core.etc.Constants;
//import network.thunder.core.etc.MockWallet;
//import network.thunder.core.mesh.Node;
//import org.bitcoinj.core.Sha256Hash;
//import org.bitcoinj.core.Transaction;
//import org.bitcoinj.core.TransactionOutPoint;
//import org.bitcoinj.crypto.TransactionSignature;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.beans.PropertyVetoException;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//
//import static org.junit.Assert.*;
//
///**
// * Created by matsjerratsch on 12/11/15.
// */
//public class LightningChannelManagementHandlerTest {
//    static ArrayList<PubkeyChannelObject> pubkeyChannelObjectArrayList = new ArrayList<>();
//    static ArrayList<PubkeyIPObject> pubkeyIPObjectArrayList = new ArrayList<>();
//    static ArrayList<ChannelStatusObject> channelStatusObjectArrayList = new ArrayList<>();
//    EmbeddedChannel channel1;
//    EmbeddedChannel channel2;
//    Message m;
//    P2PContext context;
//    Node node1;
//    Node node2;
//
//    LightningChannelManagementHandler handler1;
//    LightningChannelManagementHandler handler2;
//
//    MockWallet wallet1 = new MockWallet(Constants.getNetwork());
//    MockWallet wallet2 = new MockWallet(Constants.getNetwork());
//
//    HashMap<TransactionOutPoint, Integer> lockedOutputs1 = new HashMap<>();
//    HashMap<TransactionOutPoint, Integer> lockedOutputs2 = new HashMap<>();
//
//    @Before
//    public void prepare () throws PropertyVetoException, SQLException {
//
//        context = new P2PContext(8992);
//        context.balance = 100000;
//
//        node1 = new Node();
//        node1.context = context;
//
//        node2 = new Node();
//        node2.context = context;
//
//        handler1 = new LightningChannelManagementHandler(lockedOutputs1, wallet1, false, node1);
//        handler2 = new LightningChannelManagementHandler(lockedOutputs2, wallet2, true, node2);
//
//        channel1 = new EmbeddedChannel(handler1);
//        channel2 = new EmbeddedChannel(handler2);
//
//        m = (Message) channel2.readOutbound();
//        assertNull(m);
//    }
//
//    @Test
//    public void shouldSendMessageA () {
//        Message m;
//        m = (Message) channel1.readOutbound();
//        assertEquals(m.type, Type.ESTABLISH_CHANNEL_A);
//
//        Channel newChannel = handler1.newChannel;
//
//        //Should send keys, secretHash and amounts
//        EstablishChannelMessageA message = new Gson().fromJson(m.data, EstablishChannelMessageA.class);
//        assertTrue(Arrays.equals(message.getPubKey(), newChannel.getKeyServer().getPubKey()));
//        assertTrue(Arrays.equals(message.getPubKeyFE(), newChannel.getKeyServerA().getPubKey()));
//        assertTrue(Arrays.equals(message.getSecretHashFE(), newChannel.getAnchorSecretHashServer()));
//        assertEquals(message.getClientAmount(), newChannel.getAmountClient());
//        assertEquals(message.getServerAmount(), newChannel.getAmountServer());
//
//    }
//
//    @Test
//    public void shouldSendMessageB () {
//        Message m;
//        m = (Message) channel1.readOutbound();
//        channel2.writeInbound(m);
//        m = (Message) channel2.readOutbound();
//        assertEquals(m.type, Type.ESTABLISH_CHANNEL_B);
//
//        Channel newChannel = handler2.newChannel;
//
//        //Should send keys, secretHash and amounts
//        EstablishChannelMessageB message = new Gson().fromJson(m.data, EstablishChannelMessageB.class);
//        assertTrue(Arrays.equals(message.getPubKey(), newChannel.getKeyServer().getPubKey()));
//        assertTrue(Arrays.equals(message.getPubKeyFE(), newChannel.getKeyServerA().getPubKey()));
//        assertTrue(Arrays.equals(message.getSecretHashFE(), newChannel.getAnchorSecretHashServer()));
//        assertTrue(Arrays.equals(message.getAnchorHash(), newChannel.getAnchorTransactionServer(null, null).getHash().getBytes()));
//        assertEquals(message.getServerAmount(), newChannel.getAmountServer());
//
//    }
//
//    @Test
//    public void shouldSendMessageC () {
//        Message m;
//        m = (Message) channel1.readOutbound();
//        channel2.writeInbound(m);
//        m = (Message) channel2.readOutbound();
//        channel1.writeInbound(m);
//        m = (Message) channel1.readOutbound();
//
//        assertEquals(m.type, Type.ESTABLISH_CHANNEL_C);
//
//        Channel newChannel1 = handler1.newChannel;
//        Channel newChannel2 = handler2.newChannel;
//
//        //Should send keys, secretHash and amounts
//        EstablishChannelMessageC message = new Gson().fromJson(m.data, EstablishChannelMessageC.class);
//        assertTrue(Arrays.equals(message.getAnchorHash(), newChannel1.getAnchorTransactionServer(null, null).getHash().getBytes()));
//
//        Transaction escape = newChannel2.getEscapeTransactionServer();
//        Transaction fastEscape = newChannel2.getFastEscapeTransactionServer();
//
//        Sha256Hash hashEscape = escape.hashForSignature(0, newChannel2.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
//        Sha256Hash hashFastEscape = fastEscape.hashForSignature(0, newChannel2.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
//
//        assertTrue(newChannel2.getKeyClientA().verify(hashEscape, TransactionSignature.decodeFromBitcoin(message.getSignatureE(), true)));
//        assertTrue(newChannel2.getKeyClientA().verify(hashFastEscape, TransactionSignature.decodeFromBitcoin(message.getSignatureFE(), true)));
//
//    }
//
//    @Test
//    public void shouldSendMessageD () {
//        Message m;
//        m = (Message) channel1.readOutbound();
//        channel2.writeInbound(m);
//        m = (Message) channel2.readOutbound();
//        channel1.writeInbound(m);
//        m = (Message) channel1.readOutbound();
//        channel2.writeInbound(m);
//        m = (Message) channel2.readOutbound();
//        assertEquals(m.type, Type.ESTABLISH_CHANNEL_D);
//
//        Channel newChannel = handler1.newChannel;
//
//        //Should send keys, secretHash and amounts
//        EstablishChannelMessageD message = new Gson().fromJson(m.data, EstablishChannelMessageD.class);
//
//        Transaction escape = newChannel.getEscapeTransactionServer();
//        Transaction fastEscape = newChannel.getFastEscapeTransactionServer();
//
//        Sha256Hash hashEscape = escape.hashForSignature(0, newChannel.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
//        Sha256Hash hashFastEscape = fastEscape.hashForSignature(0, newChannel.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
//
//        assertTrue(newChannel.getKeyClientA().verify(hashEscape, TransactionSignature.decodeFromBitcoin(message.getSignatureE(), true)));
//        assertTrue(newChannel.getKeyClientA().verify(hashFastEscape, TransactionSignature.decodeFromBitcoin(message.getSignatureFE(), true)));
//    }
//
//    @Test
//    public void shouldNotAcceptSignature () {
//        Message m;
//        m = (Message) channel1.readOutbound();
//        channel2.writeInbound(m);
//        m = (Message) channel2.readOutbound();
//        channel1.writeInbound(m);
//        m = (Message) channel1.readOutbound();
//
//        EstablishChannelMessageC message = new Gson().fromJson(m.data, EstablishChannelMessageC.class);
//        EstablishChannelMessageC messageSwappedSign = new EstablishChannelMessageC(message.getAnchorHash(), message.getSignatureFE(), message.getSignatureE());
//        m = new Message(messageSwappedSign, m.type);
//
//        channel2.writeInbound(m);
//        m = (Message) channel2.readOutbound();
//        assertEquals(m.type, Type.FAILURE);
//    }
//
//}