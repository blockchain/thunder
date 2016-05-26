package network.thunder.core.communication.layer;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.Connection;
import network.thunder.core.communication.ConnectionRegistry;
import network.thunder.core.helper.callback.Command;

public class ConnectionProcessor extends AuthenticatedProcessor implements Connection {
    ConnectionRegistry connectionRegistry;
    ClientObject node;
    MessageExecutor messageExecutor;

    boolean connectionClosed = false;

    public ConnectionProcessor (ContextFactory contextFactory, ClientObject clientObject) {
        this.node = clientObject;
        this.connectionRegistry = contextFactory.getConnectionRegistry();
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        messageExecutor.sendNextLayerActive();
        setNode(node.nodeKey);
        this.messageExecutor = messageExecutor;
        connectionRegistry.onConnected(getNode(), this);
        node.onConnectionComplete.stream().forEach(Command::execute);
    }

    @Override
    public void close () {
        notifyRegistry();
        messageExecutor.closeConnection();
    }

    @Override
    public void onLayerClose () {
        notifyRegistry();
    }

    private synchronized void notifyRegistry () {
        if (!connectionClosed) {
            //Only call onDisconnected once
            connectionClosed = true;
            connectionRegistry.onDisconnected(getNode());
        }
    }

    //We don't intercept any messages here..
    @Override
    public boolean consumesInboundMessage (Object object) {
        return false;
    }

    @Override
    public boolean consumesOutboundMessage (Object object) {
        return false;
    }
}
