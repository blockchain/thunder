package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.Type;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.objects.p2p.P2PDataObject;
import network.thunder.core.communication.objects.p2p.sync.ChannelStatusObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyChannelObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyIPObject;
import network.thunder.core.database.DatabaseHandler;
import network.thunder.core.mesh.Node;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class SyncHandlerTest {

    static ArrayList<PubkeyChannelObject> pubkeyChannelObjectArrayList = new ArrayList<>();
    static ArrayList<PubkeyIPObject> pubkeyIPObjectArrayList = new ArrayList<>();
    static ArrayList<ChannelStatusObject> channelStatusObjectArrayList = new ArrayList<>();
    EmbeddedChannel channel1;
    EmbeddedChannel channel2;
    Message m;
    P2PContext context;
    Node node1;
    Node node2;

    @BeforeClass
    public static void buildDatabase () throws PropertyVetoException, SQLException {
        Connection conn = DatabaseHandler.getDataSource().getConnection();

        //TODO: Think about doing this in a separate database?

        //Clean up database before the test..
        conn.createStatement().execute("DELETE FROM channels;");
        conn.createStatement().execute("DELETE FROM channel_status;");
        conn.createStatement().execute("DELETE FROM nodes;");
        conn.createStatement().execute("DELETE FROM pubkey_ips;");

        for (int i = 1; i < 10000; i++) {
            PubkeyChannelObject pubkeyChannelObject = PubkeyChannelObject.getRandomObject();
            PubkeyIPObject pubkeyIPObject1 = PubkeyIPObject.getRandomObject();
            PubkeyIPObject pubkeyIPObject2 = PubkeyIPObject.getRandomObject();
            ChannelStatusObject channelStatusObject = ChannelStatusObject.getRandomObject();

            pubkeyIPObject1.pubkey = pubkeyChannelObject.pubkeyA;
            pubkeyIPObject2.pubkey = pubkeyChannelObject.pubkeyB;

            channelStatusObject.pubkeyA = pubkeyChannelObject.pubkeyA;
            channelStatusObject.pubkeyB = pubkeyChannelObject.pubkeyB;

            DatabaseHandler.newChannel(conn, pubkeyChannelObject);
            DatabaseHandler.newIPObject(conn, pubkeyIPObject1);
            DatabaseHandler.newIPObject(conn, pubkeyIPObject2);
            DatabaseHandler.newChannelStatus(conn, channelStatusObject);

            pubkeyChannelObjectArrayList.add(pubkeyChannelObject);
            pubkeyIPObjectArrayList.add(pubkeyIPObject1);
            pubkeyIPObjectArrayList.add(pubkeyIPObject2);
            channelStatusObjectArrayList.add(channelStatusObject);
        }
    }

    @Before
    public void prepare () throws PropertyVetoException, SQLException {

        context = new P2PContext(8992);
        context.needsInitialSyncing = true;

        node1 = new Node();
        node1.conn = DatabaseHandler.getDataSource().getConnection();

        node2 = new Node();
        node2.conn = DatabaseHandler.getDataSource().getConnection();

        context.connectedNodes.add(node1);
        context.connectedNodes.add(node2);

        channel1 = new EmbeddedChannel(new SyncHandler(false, node1, context));
        channel2 = new EmbeddedChannel(new SyncHandler(true, node2, context));

        m = (Message) channel2.readOutbound();
        assertNull(m);
    }

    @Test
    public void testSyncingProcess () {

        ArrayList<PubkeyChannelObject> pubkeyChannelObjectArrayListReceived = new ArrayList<>();
        ArrayList<PubkeyIPObject> pubkeyIPObjectArrayListReceived = new ArrayList<>();
        ArrayList<ChannelStatusObject> channelStatusObjectArrayListReceived = new ArrayList<>();

        while (!context.syncDatastructure.fullySynchronized()) {

            Message m1 = (Message) channel1.readOutbound();
            assertNotNull(m1);
            assertEquals(m1.type, Type.SYNC_GET_FRAGMENT);

            channel2.writeInbound(m1);
            Message m2 = (Message) channel2.readOutbound();
            assertEquals(m2.type, Type.SYNC_SEND_FRAGMENT);

            channel1.writeInbound(m2);
        }

        for (P2PDataObject o : context.syncDatastructure.fullDataList) {
            if (o instanceof ChannelStatusObject) {
                channelStatusObjectArrayListReceived.add((ChannelStatusObject) o);
            }
            if (o instanceof PubkeyIPObject) {
                pubkeyIPObjectArrayListReceived.add((PubkeyIPObject) o);
            }
            if (o instanceof PubkeyChannelObject) {
                pubkeyChannelObjectArrayListReceived.add((PubkeyChannelObject) o);
            }
        }

        //Check whether we actually received all objects
        assertTrue(pubkeyChannelObjectArrayList.containsAll(pubkeyChannelObjectArrayListReceived));
        assertTrue(pubkeyIPObjectArrayList.containsAll(pubkeyIPObjectArrayListReceived));
        assertTrue(channelStatusObjectArrayList.containsAll(channelStatusObjectArrayListReceived));

        //Also don't want any strange objects in here..
        assertTrue(pubkeyChannelObjectArrayListReceived.containsAll(pubkeyChannelObjectArrayList));
        assertTrue(pubkeyIPObjectArrayListReceived.containsAll(pubkeyIPObjectArrayList));
        assertTrue(channelStatusObjectArrayListReceived.containsAll(channelStatusObjectArrayList));

    }

}