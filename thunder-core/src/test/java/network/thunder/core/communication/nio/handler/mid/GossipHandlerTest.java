//package network.thunder.core.communication.nio.handler.mid;
//
//import com.google.gson.Gson;
//import io.netty.channel.embedded.EmbeddedChannel;
//import network.thunder.core.communication.Message;
//import network.thunder.core.communication.Type;
//import network.thunder.core.communication.nio.P2PContext;
//import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyChannelObject;
//import network.thunder.core.database.DatabaseHandler;
//import network.thunder.core.etc.Tools;
//import network.thunder.core.mesh.Node;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.beans.PropertyVetoException;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Random;
//
//import static org.junit.Assert.*;
//
///**
// * Created by matsjerratsch on 27/10/2015.
// */
//public class GossipHandlerTest {
//
//    EmbeddedChannel channel1;
//    EmbeddedChannel channel2;
//    Message m;
//    P2PContext context;
//    Node node1;
//    Node node2;
//
//    @Before
//    public void prepare () throws PropertyVetoException, SQLException {
//        context = new P2PContext(8992);
//
//        node1 = new Node();
//        node1.conn = DatabaseHandler.getDataSource().getConnection();
//
//        node2 = new Node();
//        node2.conn = DatabaseHandler.getDataSource().getConnection();
//
//        context.connectedNodes.add(node1);
//        context.connectedNodes.add(node2);
//
//        channel1 = new EmbeddedChannel(new GossipHandler(false, node1, context));
//        channel2 = new EmbeddedChannel(new GossipHandler(false, node2, context));
//
//        m = (Message) channel1.readOutbound();
//        assertEquals("", Type.GOSSIP_SEND_IP_OBJECT, m.type);
//
//        m = (Message) channel2.readOutbound();
//        assertEquals("", Type.GOSSIP_SEND_IP_OBJECT, m.type);
//    }
//
//    @Test
//    public void shouldAskForDataAfterInv () throws Exception {
//
//        ArrayList<byte[]> invList = new ArrayList<>();
//        for (int i = 0; i < 20; i++) {
//            byte[] b = new byte[20];
//            new Random().nextBytes(b);
//            invList.add(b);
//        }
//
//        InvObject invObject = new InvObject();
//        invObject.inventoryList = invList;
//
//        channel1.writeInbound(new Message(invObject, Type.GOSSIP_INV));
//
//        m = (Message) channel1.readOutbound();
//        assertEquals("", Type.GOSSIP_GET, m.type);
//
//        GetDataObject getDataObject = new Gson().fromJson(m.data, GetDataObject.class);
//
//        for (int i = 0; i < invList.size(); i++) {
//            System.out.println(Tools.bytesToHex(invList.get(i)));
//            System.out.println(Tools.bytesToHex(getDataObject.inventoryList.get(i)));
//
//            assertTrue(Arrays.equals(getDataObject.inventoryList.get(i), invList.get(i)));
//        }
//        channel1.checkException();
//
//    }
//
//    @Test
//    public void shouldNotSendDataToNextPeer () throws Exception {
//        ArrayList<DataObject> dataList = new ArrayList<>();
//        for (int i = 0; i < Node.THRESHHOLD_INVENTORY_AMOUNT_TO_SEND - 1; i++) {
//            dataList.add(new DataObject(PubkeyChannelObject.getRandomObject()));
//        }
//        SendDataObject sendDataObject = new SendDataObject();
//        sendDataObject.dataObjects = dataList;
//
//        channel1.writeInbound(new Message(sendDataObject, Type.GOSSIP_SEND));
//        channel1.writeInbound(new Message(sendDataObject, Type.GOSSIP_SEND));
//
//        m = (Message) channel2.readOutbound();
//        assertNull(m);
//
//    }
//
//    @Test
//    public void shouldSendDataToNextPeer () throws Exception {
//        ArrayList<DataObject> dataList = new ArrayList<>();
//        for (int i = 0; i < Node.THRESHHOLD_INVENTORY_AMOUNT_TO_SEND + 1; i++) {
//            dataList.add(new DataObject(PubkeyChannelObject.getRandomObject()));
//        }
//        SendDataObject sendDataObject = new SendDataObject();
//        sendDataObject.dataObjects = dataList;
//
//        channel1.writeInbound(new Message(sendDataObject, Type.GOSSIP_SEND));
//
//        m = (Message) channel2.readOutbound();
//        assertEquals(m.type, Type.GOSSIP_INV);
//
//        InvObject invObject = new Gson().fromJson(m.data, InvObject.class);
//
//        //Assume we don't have any of the data..
//        GetDataObject getDataObject = new GetDataObject();
//        getDataObject.inventoryList = invObject.inventoryList;
//
//        channel2.writeInbound(new Message(getDataObject, Type.GOSSIP_GET));
//
//        m = (Message) channel2.readOutbound();
//        assertEquals(m.type, Type.GOSSIP_SEND);
//
//        SendDataObject sendDataObject1 = new Gson().fromJson(m.data, SendDataObject.class);
//        for (int i = 0; i < Node.THRESHHOLD_INVENTORY_AMOUNT_TO_SEND + 1; i++) {
//            assertEquals(sendDataObject1.dataObjects.get(i).getObject(), dataList.get(i).getObject());
//        }
//
//    }
//
//    @Test
//    public void shouldSendDataToNextPeerAfterMultipleSends () throws Exception {
//        ArrayList<DataObject> dataList = new ArrayList<>();
//        ArrayList<DataObject> totalDataList = new ArrayList<>();
//        for (int i = 0; i < Node.THRESHHOLD_INVENTORY_AMOUNT_TO_SEND - 1; i++) {
//            DataObject o = new DataObject(PubkeyChannelObject.getRandomObject());
//            dataList.add(o);
//            totalDataList.add(o);
//        }
//        SendDataObject sendDataObject = new SendDataObject();
//        sendDataObject.dataObjects = dataList;
//
//        channel1.writeInbound(new Message(sendDataObject, Type.GOSSIP_SEND));
//
//        m = (Message) channel2.readOutbound();
//        assertNull(m);
//
//        dataList = new ArrayList<>();
//        for (int i = 0; i < Node.THRESHHOLD_INVENTORY_AMOUNT_TO_SEND - 1; i++) {
//            DataObject o = new DataObject(PubkeyChannelObject.getRandomObject());
//            dataList.add(o);
//            totalDataList.add(o);
//        }
//        sendDataObject = new SendDataObject();
//        sendDataObject.dataObjects = dataList;
//
//        channel1.writeInbound(new Message(sendDataObject, Type.GOSSIP_SEND));
//
//        m = (Message) channel2.readOutbound();
//        assertEquals(m.type, Type.GOSSIP_INV);
//
//        InvObject invObject = new Gson().fromJson(m.data, InvObject.class);
//        System.out.println(invObject.inventoryList.size());
//
//        //Assume we don't have any of the data..
//        GetDataObject getDataObject = new GetDataObject();
//        getDataObject.inventoryList = invObject.inventoryList;
//
//        for (byte[] a : getDataObject.inventoryList) {
//            boolean found = false;
//            for (DataObject b : totalDataList) {
//                if (Arrays.equals(a, b.getObject().getHash())) {
//                    found = true;
//                    break;
//                }
//            }
//            assertTrue(found);
//        }
//
//        channel2.writeInbound(new Message(getDataObject, Type.GOSSIP_GET));
//
//        m = (Message) channel2.readOutbound();
//        assertEquals(m.type, Type.GOSSIP_SEND);
//
//        SendDataObject sendDataObject1 = new Gson().fromJson(m.data, SendDataObject.class);
//        System.out.println(sendDataObject1.dataObjects.size());
//
//        for (DataObject d1 : totalDataList) {
//            boolean found = false;
//            for (DataObject d2 : sendDataObject1.dataObjects) {
//                if (Arrays.equals(d1.getObject().getHash(), d2.getObject().getHash())) {
//                    found = true;
//                    break;
//                }
//            }
//            assertTrue(found);
//
//        }
//
//    }
//
//}