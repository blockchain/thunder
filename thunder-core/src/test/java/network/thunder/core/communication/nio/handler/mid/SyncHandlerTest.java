package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.processor.implementations.sync.SynchronizationHelper;
import network.thunder.core.etc.MockContextFactory;
import network.thunder.core.etc.SyncDBHandlerMock;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class SyncHandlerTest {

    EmbeddedChannel channel1;
    EmbeddedChannel channel2;

    NodeClient node1;
    NodeClient node2;

    NodeServer nodeServer1 = new NodeServer();
    NodeServer nodeServer2 = new NodeServer();

    ContextFactory contextFactory1;
    ContextFactory contextFactory2;

    SyncDBHandlerMock dbHandler1;
    SyncDBHandlerMock dbHandler2;

    public void buildDatabase () {

        dbHandler1 = new SyncDBHandlerMock();

        dbHandler2 = new SyncDBHandlerMock();
        dbHandler2.fillWithRandomData();

    }

    public void prepare () {
        node1 = new NodeClient();
        node2 = new NodeClient();

        node1.isServer = false;
        node2.isServer = true;

        contextFactory1 = new MockContextFactory(nodeServer1, dbHandler1);
        contextFactory2 = new MockContextFactory(nodeServer2, dbHandler2);

        channel1 = new EmbeddedChannel(new ProcessorHandler(contextFactory1.getSyncProcessor(node1), "Sync1"));
        channel2 = new EmbeddedChannel(new ProcessorHandler(contextFactory2.getSyncProcessor(node2), "Sync2"));

        Message m = (Message) channel2.readOutbound();
        assertNull(m);
    }

    @After
    public void after () {
        channel1.checkException();
        channel2.checkException();
    }

    @Test
    public void testSyncingDataObjects () {
        buildDatabase();
        prepare();

        SynchronizationHelper syncStructure1 = contextFactory1.getSyncHelper();

        while (!syncStructure1.fullySynchronized()) {
            Message m1 = (Message) channel1.readOutbound();
            channel2.writeInbound(m1);
            Message m2 = (Message) channel2.readOutbound();
            channel1.writeInbound(m2);
        }

        assertNull(channel1.readOutbound());
        assertNull(channel2.readOutbound());

        syncStructure1.saveFullSyncToDatabase();

        assertTrue(checkListsEqual(dbHandler1.channelStatusObjectArrayList, dbHandler2.channelStatusObjectArrayList));
        assertTrue(checkListsEqual(dbHandler1.pubkeyChannelObjectArrayList, dbHandler2.pubkeyChannelObjectArrayList));
        assertTrue(checkListsEqual(dbHandler1.pubkeyIPObjectArrayList, dbHandler2.pubkeyIPObjectArrayList));
        assertTrue(checkListsEqual(dbHandler1.totalList, dbHandler2.totalList));

    }

    public boolean checkListsEqual (List list1, List list2) {
        return list1.containsAll(list2) && list2.containsAll(list1);
    }

}