package network.thunder.core.communication.processor.implementations;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.interfaces.factories.SyncMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.sync.Sync;
import network.thunder.core.communication.objects.messages.interfaces.message.sync.types.SyncGetMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.sync.types.SyncSendMessage;
import network.thunder.core.communication.objects.p2p.P2PDataObject;
import network.thunder.core.communication.objects.p2p.SynchronizationHelper;
import network.thunder.core.communication.processor.interfaces.SyncProcessor;
import network.thunder.core.mesh.Node;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class SyncProcessorImpl implements SyncProcessor {
    SyncMessageFactory messageFactory;
    Node node;
    SynchronizationHelper syncStructure;

    MessageExecutor messageExecutor;

    int lastIndex;

    public SyncProcessorImpl (SyncMessageFactory messageFactory, Node node, SynchronizationHelper syncStructure) {
        this.messageFactory = messageFactory;
        this.node = node;
        this.syncStructure = syncStructure;
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
    public void onInboundMessageMessage (Message message) {
        if (message instanceof Sync) {
            if (message instanceof SyncSendMessage) {
                processSyncSendMessage(message);
            } else if (message instanceof SyncGetMessage) {
                processSyncGetMessage(message);
            }
        } else {
            messageExecutor.sendMessageDownwards(message);
        }
    }

    @Override
    public void onOutboundMessage (Message message) {
        //TODO: Maybe change the handler to be duplex and handle the passthrough here?
    }

    public boolean shouldSync () {
        return !node.isServer && !synchronizationComplete();
    }

    private void startSyncing () {
        if (node.justFetchNewIpAddresses) {
            sendGetIPs();
        } else {
            sendGetNextSyncData();
        }
    }

    private void processSyncSendMessage (Message message) {
        SyncSendMessage syncMessage = (SyncSendMessage) message;
        if (node.justFetchNewIpAddresses) {
            syncStructure.newIPList(syncMessage.getSyncData());
            messageExecutor.closeConnection();
        } else {
            syncStructure.newFragment(lastIndex, syncMessage.getSyncData());

            if (!syncStructure.fullySynchronized()) {
                sendGetNextSyncData();
            }
        }
    }

    private void processSyncGetMessage (Message message) {
        SyncGetMessage syncMessage = (SyncGetMessage) message;
        if (syncMessage.getIPs()) {
            sendSyncIPs();
        } else {
            sendSyncData(syncMessage.getFragmentIndex());
        }
    }

    private void sendGetIPs () {
        Message syncGetIPs = messageFactory.getSyncSendIPMessage();
        messageExecutor.sendMessageUpwards(syncGetIPs);
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

    private void sendSyncIPs () {
        List<P2PDataObject> ipList = syncStructure.getIPAddresses();
        Message syncIpMessage = messageFactory.getSyncSendMessage(ipList);
        messageExecutor.sendMessageUpwards(syncIpMessage);
    }

    private boolean synchronizationComplete () {
        return syncStructure.fullySynchronized();
    }
}
