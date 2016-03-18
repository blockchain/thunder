package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.messages.impl.LNEventHelperImpl;
import network.thunder.core.communication.objects.messages.impl.factories.ContextFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.impl.results.*;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ConnectionResult;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ResultCommand;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.communication.processor.implementations.sync.SynchronizationHelper;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Wallet;

import java.util.ArrayList;
import java.util.List;

import static network.thunder.core.communication.processor.ChannelIntent.*;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public class ConnectionManagerImpl implements ConnectionManager {
    public final static int NODES_TO_SYNC = 5;
    public final static int CHANNELS_TO_OPEN = 5;
    public final static int MINIMUM_AMOUNT_OF_IPS = 10;

    NodeServer node;

    ContextFactory contextFactory;
    DBHandler dbHandler;

    LNEventHelper eventHelper;

    P2PServer server;

    public ConnectionManagerImpl (NodeServer node, Wallet wallet, DBHandler dbHandler) {
        this.dbHandler = dbHandler;
        this.node = node;
        eventHelper = new LNEventHelperImpl();
        contextFactory = new ContextFactoryImpl(node, dbHandler, wallet, eventHelper);
    }

    public ConnectionManagerImpl (NodeServer node, ContextFactory contextFactory, DBHandler dbHandler, LNEventHelper eventHelper) {
        this.node = node;
        this.contextFactory = contextFactory;
        this.dbHandler = dbHandler;
        this.eventHelper = eventHelper;
    }

    @Override
    public void startUp (ResultCommand callback) {
        new Thread(new Runnable() {
            @Override
            public void run () {
                try {
                    startUpBlocking(callback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void startUpBlocking (ResultCommand callback) throws Exception {
        startListening(callback);
        connectOpenChannels();
        fetchNetworkIPs(callback);
        startBuildingRandomChannel(callback);
    }

    public void startListening (ResultCommand callback) {
        System.out.println("startListening " + this.node.portServer);
        server = new P2PServer(contextFactory);
        server.startServer(this.node.portServer);
        callback.execute(new SuccessResult());
    }

    private void connectOpenChannels () {
        //TODO
    }

    @Override
    public void fetchNetworkIPs (ResultCommand callback) {
        new Thread(() -> {
            fetchNetworkIPsBlocking(callback);
        }).start();
    }

    private void fetchNetworkIPsBlocking (ResultCommand callback) {
        List<PubkeyIPObject> ipList = dbHandler.getIPObjects();
        List<PubkeyIPObject> alreadyFetched = new ArrayList<>();
        List<PubkeyIPObject> seedNodes = SeedNodes.getSeedNodes();
        ipList.addAll(seedNodes);

        while (ipList.size() < MINIMUM_AMOUNT_OF_IPS) {
            try {
                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, alreadyFetched);
                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, node.pubKeyServer.getPubKey());

                if (ipList.size() == 0) {
                    break;
                }

                PubkeyIPObject randomNode = Tools.getRandomItemFromList(ipList);
                NodeClient client = ipObjectToNode(randomNode, GET_IPS);
                client.resultCallback = new NullResultCommand();
                connectBlocking(client);
                alreadyFetched.add(randomNode);

                ipList = dbHandler.getIPObjects();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                    Thread.sleep(5000);
                    continue;
                }

                PubkeyIPObject randomNode = Tools.getRandomItemFromList(ipList);

                NodeClient node = ipObjectToNode(randomNode, OPEN_CHANNEL);
                buildChannel(node.pubKeyClient.getPubKey(), callback);

                alreadyTried.add(randomNode);

                alreadyConnected = dbHandler.getIPObjectsWithActiveChannel();

                Thread.sleep(5000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO be able to tear down a channel completely again
    //TODO reset sleepIntervall if
    @Override
    public void buildChannel (byte[] nodeKey, ResultCommand callback) {
        new Thread(() -> {
            final long[] sleepIntervall = {1000};
            final boolean[] reconnectAutomatically = {true};
            while (reconnectAutomatically[0]) {

                PubkeyIPObject ipObject = dbHandler.getIPObject(nodeKey);
                if (ipObject != null) {
                    NodeClient node1 = ipObjectToNode(ipObject, OPEN_CHANNEL);
                    node1.resultCallback = result -> {
                        if(result instanceof ConnectionResult) {
                            ConnectionResult result1 = (ConnectionResult) result;
                            if (result1.shouldTryToReconnect()) {
                                reconnectAutomatically[0] = true;
                            }
                            if (result.wasSuccessful()) {
                                sleepIntervall[0] = 1000;
                            }
                            callback.execute(result);
                        }
                    };

                    connectBlocking(node1);
                }
                sleepIntervall[0] *= 1.2;
                try {
                    sleepIntervall[0] = Math.min(sleepIntervall[0], 5 * 60 * 1000);
                    Thread.sleep(sleepIntervall[0]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void startSyncing (ResultCommand callback) {
        new Thread(() -> {
            startSyncingBlocking(callback);
        }).start();
    }

    private void startSyncingBlocking (ResultCommand callback) {
        SynchronizationHelper synchronizationHelper = contextFactory.getSyncHelper();
        List<PubkeyIPObject> ipList = dbHandler.getIPObjects();
        List<PubkeyIPObject> alreadyFetched = new ArrayList<>();
        List<PubkeyIPObject> seedNodes = SeedNodes.getSeedNodes();
        ipList.addAll(seedNodes);
        while (!synchronizationHelper.fullySynchronized()) {

            try {
                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, alreadyFetched);
                ipList = PubkeyIPObject.removeFromListByPubkey(ipList, node.pubKeyServer.getPubKey());

                if (ipList.size() == 0) {
                    callback.execute(new NoSyncResult());
                    return;
                }

                PubkeyIPObject randomNode = Tools.getRandomItemFromList(ipList);
                NodeClient client = ipObjectToNode(randomNode, GET_SYNC_DATA);
                connectBlocking(client);
                alreadyFetched.add(randomNode);

                ipList = dbHandler.getIPObjects();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        callback.execute(new SyncSuccessResult());
    }

    private void connect (NodeClient node) {
        P2PClient client = new P2PClient(contextFactory);
        client.connectTo(node);
    }

    private void connectBlocking (NodeClient node) {
        try {
            P2PClient client = new P2PClient(contextFactory);
            client.connectBlocking(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private NodeClient ipObjectToNode (PubkeyIPObject ipObject, ChannelIntent intent) {
        NodeClient node = new NodeClient();
        node.isServer = false;
        node.intent = intent;
        node.pubKeyClient = ECKey.fromPublicOnly(ipObject.pubkey);
        node.host = ipObject.IP;
        node.port = ipObject.port;
        return node;
    }

}
