package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.messages.impl.factories.SyncMessageFactoryImpl;
import network.thunder.core.communication.objects.messages.interfaces.factories.SyncMessageFactory;
import network.thunder.core.communication.processor.implementations.sync.SynchronizationHelper;
import network.thunder.core.communication.processor.implementations.sync.SyncProcessorImpl;
import network.thunder.core.communication.processor.interfaces.SyncProcessor;
import network.thunder.core.etc.SyncDBHandlerMock;
import network.thunder.core.mesh.Node;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class SyncHandlerTest {

    EmbeddedChannel channel1;
    EmbeddedChannel channel2;

    Node node1;
    Node node2;

    SyncMessageFactory messageFactory;

    SyncProcessor syncProcessor1;
    SyncProcessor syncProcessor2;

    SynchronizationHelper syncStructure1;
    SynchronizationHelper syncStructure2;

    SyncDBHandlerMock handler1;
    SyncDBHandlerMock handler2;

    public void buildDatabase () {

        handler1 = new SyncDBHandlerMock();

        handler2 = new SyncDBHandlerMock();
        handler2.fillWithRandomData();

    }

    public void prepare (boolean shouldOnlyFetchIPs) {
        node1 = new Node();
        node2 = new Node();

        node1.isServer = false;
        node1.justFetchNewIpAddresses = shouldOnlyFetchIPs;

        node2.isServer = true;

        messageFactory = new SyncMessageFactoryImpl();

        syncStructure1 = new SynchronizationHelper(handler1);
        syncStructure2 = new SynchronizationHelper(handler2);

        syncProcessor1 = new SyncProcessorImpl(messageFactory, node1, syncStructure1);
        syncProcessor2 = new SyncProcessorImpl(messageFactory, node2, syncStructure2);

        channel1 = new EmbeddedChannel(new ProcessorHandler(syncProcessor1, "Sync1"));
        channel2 = new EmbeddedChannel(new ProcessorHandler(syncProcessor2, "Sync2"));

        Message m = (Message) channel2.readOutbound();
        assertNull(m);
    }

    @Test
    public void testSyncingIPObjects () {
        buildDatabase();
        prepare(true);

        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());

        assertEquals(100, handler1.pubkeyIPObjectArrayList.size());
    }

    @Test
    public void testSyncingDataObjects () {
        buildDatabase();
        prepare(false);

        while (!syncStructure1.fullySynchronized()) {
            Message m1 = (Message) channel1.readOutbound();
            channel2.writeInbound(m1);
            Message m2 = (Message) channel2.readOutbound();
            channel1.writeInbound(m2);
        }

        assertNull(channel1.readOutbound());
        assertNull(channel2.readOutbound());

        syncStructure1.saveFullSyncToDatabase();

        assertTrue(checkListsEqual(handler1.channelStatusObjectArrayList, handler2.channelStatusObjectArrayList));
        assertTrue(checkListsEqual(handler1.pubkeyChannelObjectArrayList, handler2.pubkeyChannelObjectArrayList));
        assertTrue(checkListsEqual(handler1.pubkeyIPObjectArrayList, handler2.pubkeyIPObjectArrayList));
        assertTrue(checkListsEqual(handler1.totalList, handler2.totalList));

    }

    public boolean checkListsEqual (List list1, List list2) {
        return list1.containsAll(list2) && list2.containsAll(list1);
    }

}