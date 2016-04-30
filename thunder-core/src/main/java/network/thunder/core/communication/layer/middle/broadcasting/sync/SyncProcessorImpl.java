package network.thunder.core.communication.layer.middle.broadcasting.sync;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.Sync;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.SyncGetMessage;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.SyncMessageFactory;
import network.thunder.core.communication.layer.middle.broadcasting.sync.messages.SyncSendMessage;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.helper.events.LNEventHelper;

import java.util.List;

public class SyncProcessorImpl extends SyncProcessor implements SyncClient {
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
        syncStructure.addSyncClient(this);
        messageExecutor.sendNextLayerActive();
    }

    @Override
    public void onLayerClose () {
        syncStructure.removeSyncClient(this);
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

    @Override
    public void syncSegment (int segment) {
        lastIndex = segment;
        SyncGetMessage getMessage = messageFactory.getSyncGetMessage(segment);
        messageExecutor.sendMessageUpwards(getMessage);
    }

    private void processSyncSendMessage (Message message) {
        SyncSendMessage syncMessage = (SyncSendMessage) message;
        syncStructure.newFragment(lastIndex, syncMessage.getDataList());
        eventHelper.onP2PDataReceived();
    }

    private void processSyncGetMessage (Message message) {
        SyncGetMessage syncMessage = (SyncGetMessage) message;
        sendSyncData(syncMessage.fragmentIndex);
    }

    private void sendSyncData (int fragmentIndex) {
        List<P2PDataObject> dataObjects = syncStructure.getFragment(fragmentIndex);
        Message syncSendMessage = messageFactory.getSyncSendMessage(dataObjects);
        messageExecutor.sendMessageUpwards(syncSendMessage);
    }
}
