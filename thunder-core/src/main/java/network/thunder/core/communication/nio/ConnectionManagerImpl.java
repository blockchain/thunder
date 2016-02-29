package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.messages.impl.LNEventHelperImpl;
import network.thunder.core.communication.objects.messages.impl.factories.ContextFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Wallet;

import java.util.ArrayList;
import java.util.List;

import static network.thunder.core.communication.processor.ChannelIntent.GET_IPS;
import static network.thunder.core.communication.processor.ChannelIntent.OPEN_CHANNEL;

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
    public void startUp () {
        new Thread(new Runnable() {
            @Override
            public void run () {
                try {
                    startUpBlocking();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void startUpBlocking () throws Exception {
        startListening();
        connectOpenChannels();
        fetchNetworkIPs();
        startBuildingRandomChannel();
    }

    public void startListening () {
        System.out.println("startListening " + this.node.portServer);
        server = new P2PServer(contextFactory);
        server.startServer(this.node.portServer);
    }

    private void connectOpenChannels () {
        //TODO
    }

    @Override
    public void fetchNetworkIPs () {
        new Thread(new Runnable() {
            @Override
            public void run () {

                List<PubkeyIPObject> ipList = dbHandler.getIPObjects();
                List<PubkeyIPObject> alreadyFetched = new ArrayList<>();
                List<PubkeyIPObject> seedNodes = SeedNodes.getSeedNodes();
                ipList.addAll(seedNodes);

                while (ipList.size() < MINIMUM_AMOUNT_OF_IPS) {
                    try {
                        ipList = PubkeyIPObject.removeFromListByPubkey(ipList, alreadyFetched);
                        ipList = PubkeyIPObject.removeFromListByPubkey(ipList, node.pubKeyServer.getPubKey());

                        if (ipList.size() == 0) {
                            System.out.println("Through with all nodes - wait to collect more nodes..");
                            Thread.sleep(60 * 10 * 1000);
                            alreadyFetched.clear();
                            ipList = dbHandler.getIPObjects();
                            continue;
                        }

                        PubkeyIPObject randomNode = Tools.getRandomItemFromList(ipList);
                        connectBlocking(randomNode, GET_IPS);
                        alreadyFetched.add(randomNode);

                        ipList = dbHandler.getIPObjects();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                ipList = dbHandler.getIPObjects();
                System.out.println("fetchNetworkIPs done. Total IPs: " + ipList.size());

            }
        }).start();
    }

    @Override
    public void startBuildingRandomChannel () {
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

                try {
                    connect(randomNode, OPEN_CHANNEL);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                alreadyTried.add(randomNode);

                alreadyConnected = dbHandler.getIPObjectsWithActiveChannel();

                Thread.sleep(5000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void buildChannel (byte[] nodeKey) {
        new Thread(new Runnable() {
            @Override
            public void run () {
                long sleepIntervall = 1000;
                while (true) {

                    PubkeyIPObject ipObject = dbHandler.getIPObject(nodeKey);
                    if (ipObject != null) {

                        try {
                            connectBlocking(ipObject, OPEN_CHANNEL);
                            sleepIntervall = 1000;
                        } catch (Exception e) {
                            sleepIntervall += 1000;
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(sleepIntervall);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void connect (PubkeyIPObject ipObject, ChannelIntent intent) throws Exception {
        P2PClient client = new P2PClient(contextFactory);
        NodeClient node = ipObjectToNode(ipObject, intent);
        client.connectTo(node);
    }

    private void connectBlocking (PubkeyIPObject ipObject, ChannelIntent intent) throws Exception {
        P2PClient client = new P2PClient(contextFactory);
        NodeClient node = ipObjectToNode(ipObject, intent);
        client.connectBlocking(node);
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
