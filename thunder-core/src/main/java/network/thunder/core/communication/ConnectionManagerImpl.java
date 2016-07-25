package network.thunder.core.communication;

import com.google.common.collect.Sets;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.middle.broadcasting.sync.SynchronizationHelper;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.communication.nio.P2PClient;
import network.thunder.core.communication.nio.P2PServer;
import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.SeedNodes;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;
import network.thunder.core.helper.callback.SyncListener;
import network.thunder.core.helper.callback.results.*;
import network.thunder.core.helper.events.LNEventHelper;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.thunder.core.communication.processor.ConnectionIntent.*;

/**
 * ConnectionManager is responsible to keep track of which connections are currently open and connect to nodes by NodeKey.
 * <p>
 * We are keeping track of open connections not on IP level, but rather on a per node basis. Similar,
 * when a new connection is requested, the ConnectionManager is in charge to look up the most recent
 * address out of the database.
 * <p>
 * TODO: When a new connection comes in from a node that we were connected already, close the old connection
 * TODO: Move Syncing and IP-Seeding to their respective classes, together with hookups for Syncer / IPSeeder interface hookups
 * TODO: Think about how we want to close channels again once we removed the ChannelIntent object in ClientObject (maybe prune connection pool?)
 */
public class ConnectionManagerImpl implements ConnectionManager, ConnectionRegistry {
    private static final Logger log = Tools.getLogger();

    public final static int CHANNELS_TO_OPEN = 5;
    public final static int MINIMUM_AMOUNT_OF_IPS = 5;
    public final static int MINIMUM_AMOUNT_NODES_FETCH_IPS_FROM = 5;

    Set<NodeKey> connectedNodes = Sets.newConcurrentHashSet();
    Set<NodeKey> currentlyConnecting = Sets.newConcurrentHashSet();
    Map<NodeKey, Connection> connectionMap = new ConcurrentHashMap<>();
    Map<NodeKey, ConnectionIntent> intentMap = new ConcurrentHashMap<>();
    Map<NodeKey, ConnectionListener> connectionListenerMap = new ConcurrentHashMap<>();
    Map<NodeKey, String> stringMap = new ConcurrentHashMap<>();

    P2PServer server;
    ServerObject node;

    DBHandler dbHandler;
    ContextFactory contextFactory;
    LNEventHelper eventHelper;
    ChannelManager channelManager;

    ExecutorService executorService = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public ConnectionManagerImpl (ContextFactory contextFactory, DBHandler dbHandler) {
        this.dbHandler = dbHandler;
        this.node = contextFactory.getServerSettings();
        this.contextFactory = contextFactory;
        this.eventHelper = contextFactory.getEventHelper();
        this.channelManager = contextFactory.getChannelManager();
    }

    @Override
    public void onConnected (NodeKey node, Connection connection) {
        //Check if there is already an open connection and close it..
        if (connectedNodes.contains(node)) {
            //closeConnection will automatically call onDisconnected..
            //TODO not viable currently, as an incoming requests completely interrupts any outgoing connection
            //Can uncomment it again when we join connections based on nodekey
            //closeConnection(node);
        }

        //Add the new connection to the pool..
        connectedNodes.add(node);
        connectionMap.put(node, connection);

        ConnectionListener listener = connectionListenerMap.get(node);
        if (listener != null) {
            listener.onSuccess.execute();
            connectionListenerMap.remove(node);
        }
        currentlyConnecting.remove(node);
    }

    @Override
    public void onDisconnected (NodeKey node) {
        connectedNodes.remove(node);
        currentlyConnecting.remove(node);
    }

    @Override
    public boolean isConnected (NodeKey node) {
        return connectedNodes.contains(node);
    }

    @Override
    public void connect (NodeKey node, ConnectionIntent intent, ConnectionListener connectionListener) {
        if (connectedNodes.contains(node)) {
            //Already connected
            overrideIntentIfNecessary(node, intent);
            connectionListener.onSuccess.execute();
        } else if (currentlyConnecting.contains(node)) {
            //TODO we are overwriting the old listener - not sure if rather multimap or normal map..
            overrideIntentIfNecessary(node, intent);
            connectionListenerMap.put(node, connectionListener);
        } else {
            currentlyConnecting.add(node);
            connectionListenerMap.put(node, connectionListener);
            stringMap.put(node, connectionListener.toString());
            intentMap.put(node, intent);

            //TODO legacy below...
            PubkeyIPObject ipObject = dbHandler.getIPObject(node.nodeKey.getPubKey());
            ClientObject clientObject = ipObjectToNode(ipObject, ConnectionIntent.MISC);
            clientObject.onAuthenticationFailed.add(() -> onAuthenticationFailed(ipObject));

            ConnectionListener internalListener = new ConnectionListener();
            internalListener.onFailure = () -> this.onDisconnected(node);

            connect(clientObject, internalListener);
        }
    }

    @Override
    public Future randomConnections (int amount, ConnectionIntent intent, ConnectionListener connectionListener) {
        return executorService.submit(new RandomConnections(amount, intent, connectionListener));
    }

    @Override
    public void disconnectByIntent (ConnectionIntent intent) {
        for (NodeKey nodeKey : connectedNodes) {
            ConnectionIntent currentIntent = intentMap.get(nodeKey);
            if (currentIntent != null) {
                if (intent.getPriority() >= currentIntent.getPriority()) {
                    closeConnection(nodeKey);
                }
            }
        }
    }

    private void closeConnection (NodeKey nodeKey) {
        Connection connection = connectionMap.get(nodeKey);
        if (connection != null) {
            connection.close();
        }
    }

    private void overrideIntentIfNecessary (NodeKey node, ConnectionIntent intent) {
        ConnectionIntent currentIntent = intentMap.get(node);
        if (currentIntent == null) {
            intentMap.put(node, intent);
        } else if (currentIntent.getPriority() < intent.getPriority()) {
            //Connection got upgraded to a higher purpose. Update intentMap to not close connection when closing down an intent.
            intentMap.put(node, intent);
        }
    }

    private void onAuthenticationFailed (PubkeyIPObject ipObject) {
        dbHandler.invalidateP2PObject(ipObject);
    }

    public void startListening (ResultCommand callback) {
        server = new P2PServer(contextFactory);
        server.startServer(this.node.portServer);
        callback.execute(new SuccessResult());
    }

    @Override
    public void fetchNetworkIPs (ResultCommand callback) {
        new Thread(() -> {
            fetchNetworkIPsBlocking(callback);
        }).start();
    }

    private void fetchNetworkIPsBlocking (ResultCommand callback) {
        dbHandler.insertIPObjects(
                SeedNodes.getSeedNodes().stream().map((Function<PubkeyIPObject, P2PDataObject>) ipObject -> ipObject).collect(Collectors.toList()));
        List<PubkeyIPObject> ipList = dbHandler.getIPObjects();
        List<PubkeyIPObject> alreadyFetched = new ArrayList<>();

        do {
            try {
                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, alreadyFetched);
                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, node.pubKeyServer.getPubKey());

                if (ipList.size() == 0) {
                    break;
                }

                PubkeyIPObject randomNode = Tools.getRandomItemFromList(ipList);
                ClientObject client = ipObjectToNode(randomNode, GET_IPS);
                client.resultCallback = new NullResultCommand();
                connect(client, new ConnectionListener());
                Thread.sleep(300);
                alreadyFetched.add(randomNode);
                ipList = dbHandler.getIPObjects();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (ipList.size() < MINIMUM_AMOUNT_OF_IPS || alreadyFetched.size() < MINIMUM_AMOUNT_NODES_FETCH_IPS_FROM);
        ipList = dbHandler.getIPObjects();

        if (ipList.size() > 0) {
            callback.execute(new SuccessResult());
        } else {
            callback.execute(new FailureResult());
        }
    }

    @Override
    public void startBuildingRandomChannel (ResultCommand callback) {
        try {
            List<PubkeyIPObject> ipList = dbHandler.getIPObjects();
            List<PubkeyIPObject> alreadyConnected = dbHandler.getIPObjectsWithActiveChannel();
            List<PubkeyIPObject> alreadyTried = new ArrayList<>();

            while (alreadyConnected.size() < CHANNELS_TO_OPEN) {
                //TODO Here we want some algorithm to determine who we want to connect to initially..

                ipList = dbHandler.getIPObjects();

                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, node.pubKeyServer.getPubKey());
                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, alreadyConnected);
                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, alreadyTried);

                if (ipList.size() == 0) {
                    callback.execute(new FailureResult());
                    return;
                }

                PubkeyIPObject randomNode = Tools.getRandomItemFromList(ipList);

                log.info("BUILD CHANNEL WITH: " + randomNode);

                ClientObject node = ipObjectToNode(randomNode, OPEN_CHANNEL);
                channelManager.openChannel(node.nodeKey, new ChannelOpenListener());

                alreadyTried.add(randomNode);

                alreadyConnected = dbHandler.getIPObjectsWithActiveChannel();

                Thread.sleep(5000);
            }
            callback.execute(new SuccessResult());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startSyncing (ResultCommand callback) {
        new Thread(() -> {
            startSyncingBlocking(callback);
        }).start();
    }

    //TODO: Right now we are blindly and fully syncing with 4 random nodes. Let this be a more distributed process, when data grows
    private void startSyncingBlocking (ResultCommand callback) {
        SynchronizationHelper synchronizationHelper = contextFactory.getSyncHelper();
        List<PubkeyIPObject> ipList = dbHandler.getIPObjects();
        List<PubkeyIPObject> alreadyFetched = new ArrayList<>();
        List<PubkeyIPObject> seedNodes = SeedNodes.getSeedNodes();
        ipList.addAll(seedNodes);
        int amountOfNodesToSyncFrom = 3;
        int totalSyncs = 0;
        while (totalSyncs < amountOfNodesToSyncFrom) {
            synchronizationHelper.resync(new SyncListener());
            ipList = PubkeyIPObject.removeFromListByPubkey(ipList, alreadyFetched);
            ipList = PubkeyIPObject.removeFromListByPubkey(ipList, node.pubKeyServer.getPubKey());

            if (ipList.size() == 0) {
                callback.execute(new NoSyncResult());
                return;
            }

            PubkeyIPObject randomNode = Tools.getRandomItemFromList(ipList);
            ClientObject client = ipObjectToNode(randomNode, GET_SYNC_DATA);
            connectBlocking(client);
            alreadyFetched.add(randomNode);
            totalSyncs++;
            ipList = dbHandler.getIPObjects();
        }
        callback.execute(new SyncSuccessResult());
    }

    private void connect (ClientObject node, ConnectionListener listener) {
        P2PClient client = new P2PClient(contextFactory);
        client.connectTo(node, listener);
    }

    private void connectBlocking (ClientObject node) {
        try {
            P2PClient client = new P2PClient(contextFactory);
            client.connectBlocking(node, new ConnectionListener());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ClientObject ipObjectToNode (PubkeyIPObject ipObject, ConnectionIntent intent) {
        ClientObject node = new ClientObject();
        node.isServer = false;
        node.intent = intent;
        node.nodeKey = new NodeKey(ipObject.pubkey);
        node.host = ipObject.hostname;
        node.port = ipObject.port;
        return node;
    }

    private class RandomConnections implements Runnable {

        public RandomConnections (int amount, ConnectionIntent intent, ConnectionListener connectionListener) {
            this.amount = amount;
            this.intent = intent;
            this.connectionListener = connectionListener;
        }

        int amount = 0;
        ConnectionIntent intent;
        ConnectionListener connectionListener;

        int connections = 0;
        int currentlyConnecting = 0;

        @Override
        public void run () {
            try {
                List<PubkeyIPObject> ipObjects = dbHandler.getIPObjects();
                while (ipObjects.size() > 0 && connections < amount) {
                    if (currentlyConnecting + connections > amount) {
                        Thread.sleep(100);
                        continue;
                    }

                    PubkeyIPObject pubkeyIPObject = Tools.getRandomItemFromList(ipObjects);
                    ipObjects.remove(pubkeyIPObject);
                    NodeKey nodeKey = new NodeKey(pubkeyIPObject.pubkey);

                    ConnectionListener connectionListener = new ConnectionListener();
                    connectionListener.onFailure = this::connectionFailed;
                    connectionListener.onSuccess = this::connectionEstablished;

                    currentlyConnecting++;
                    connect(nodeKey, intent, connectionListener);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        private void connectionEstablished () {
            connections++;
            currentlyConnecting--;
        }

        private void connectionFailed () {
            currentlyConnecting--;
        }
    }

}
