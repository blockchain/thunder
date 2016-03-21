package network.thunder.core.communication.layer.middle.broadcasting.sync;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.SyncGetMessage;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.SyncSendMessage;
import network.thunder.core.helper.callback.results.SyncSuccessResult;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.SyncMessageFactory;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.Sync;
import network.thunder.core.communication.processor.ConnectionIntent;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class SyncProcessorImpl extends SyncProcessor {
    SyncMessageFactory messageFactory;
    SynchronizationHelper syncStructure;
    LNEventHelper eventHelper;
    ClientObject node;

    MessageExecutor messageExecutor;

    int lastIndex;

    public SyncProcessorImpl (ContextFactory contextFactory, ClientObject node) {
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
            syncComplete();
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

    private void syncComplete () {
        if (node.intent == ConnectionIntent.GET_SYNC_DATA) {
            node.resultCallback.execute(new SyncSuccessResult());
            messageExecutor.closeConnection();
        } else {
            messageExecutor.sendNextLayerActive();
        }
    }

    private void processSyncSendMessage (Message message) {
        SyncSendMessage syncMessage = (SyncSendMessage) message;
        syncStructure.newFragment(lastIndex, syncMessage.getDataList());
        eventHelper.onP2PDataReceived();

        //TODO cancel the connection with some other condition as well..
        if (!syncStructure.fullySynchronized()) {
            sendGetNextSyncData();
        } else {
            syncComplete();
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
