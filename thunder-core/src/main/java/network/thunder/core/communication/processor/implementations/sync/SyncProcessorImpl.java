package network.thunder.core.communication.processor.implementations.sync;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.sync.SyncGetMessage;
import network.thunder.core.communication.objects.messages.impl.message.sync.SyncSendMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.factories.SyncMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.message.sync.Sync;
import network.thunder.core.communication.processor.interfaces.SyncProcessor;
import network.thunder.core.mesh.NodeClient;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class SyncProcessorImpl extends SyncProcessor {
    SyncMessageFactory messageFactory;
    SynchronizationHelper syncStructure;
    LNEventHelper eventHelper;
    NodeClient node;

    MessageExecutor messageExecutor;

    int lastIndex;

    public SyncProcessorImpl (ContextFactory contextFactory, NodeClient node) {
        this.messageFactory = contextFactory.getSyncMessageFactory();
        this.syncStructure = contextFactory.getSyncHelper();
        this.eventHelper = contextFactory.getEventHelper();
        this.node = node;
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
        if (shouldSync()) {
            startSyncing();
        } else {
            messageExecutor.sendNextLayerActive();
        }
    }

    @Override
    public void onInboundMessage (Message message) {
        if (message instanceof SyncSendMessage) {
            processSyncSendMessage(message);
        } else if (message instanceof SyncGetMessage) {
            processSyncGetMessage(message);
        }
    }

    @Override
    public boolean consumesInboundMessage (Object object) {
        return (object instanceof Sync);
    }

    @Override
    public boolean consumesOutboundMessage (Object object) {
        return false;
    }

    public boolean shouldSync () {
        return !node.isServer && !synchronizationComplete();
    }

    private void startSyncing () {
        sendGetNextSyncData();
    }

    private void processSyncSendMessage (Message message) {
        SyncSendMessage syncMessage = (SyncSendMessage) message;
        syncStructure.newFragment(lastIndex, syncMessage.getDataList());
        eventHelper.onP2PDataReceived();

        if (!syncStructure.fullySynchronized()) {
            sendGetNextSyncData();
        } else {
            messageExecutor.sendNextLayerActive();
        }
    }

    private void processSyncGetMessage (Message message) {
        SyncGetMessage syncMessage = (SyncGetMessage) message;
        sendSyncData(syncMessage.fragmentIndex);
    }

    private void sendGetNextSyncData () {
        lastIndex = syncStructure.getNextFragmentIndexToSynchronize();
        SyncGetMessage getMessage = messageFactory.getSyncGetMessage(lastIndex);
        messageExecutor.sendMessageUpwards(getMessage);
    }

    private void sendSyncData (int fragmentIndex) {
        List<P2PDataObject> dataObjects = syncStructure.getFragment(fragmentIndex);
        Message syncSendMessage = messageFactory.getSyncSendMessage(dataObjects);
        messageExecutor.sendMessageUpwards(syncSendMessage);
    }

    private boolean synchronizationComplete () {
        return syncStructure.fullySynchronized();
    }
}
