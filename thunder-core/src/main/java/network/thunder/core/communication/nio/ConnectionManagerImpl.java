package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.messages.impl.factories.ContextFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.Node;
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

    Node node;

    ContextFactory contextFactory;
    DBHandler dbHandler;

    P2PServer server;

    public ConnectionManagerImpl (Node node, Wallet wallet, DBHandler dbHandler) {
        this.dbHandler = dbHandler;
        this.node = node;
        contextFactory = new ContextFactoryImpl(dbHandler, wallet);
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
        startBuildingChannel();
    }

    public void startListening () throws Exception {
        System.out.println("startListening " + this.node.portServer);
        server = new P2PServer(contextFactory);
        server.startServer(this.node, this.node.portServer);
    }

    private void connectOpenChannels () {
        //TODO
    }

    private void fetchNetworkIPs () throws Exception {
        List<PubkeyIPObject> ipList = dbHandler.getIPObjects();
        List<PubkeyIPObject> alreadyFetched = new ArrayList<>();
        List<PubkeyIPObject> seedNodes = SeedNodes.getSeedNodes();
        ipList.addAll(seedNodes);

        while (ipList.size() < MINIMUM_AMOUNT_OF_IPS) {
            ipList = PubkeyIPObject.removeFromListByPubkey(ipList, alreadyFetched);
            ipList = PubkeyIPObject.removeFromListByPubkey(ipList, node.pubKeyServer.getPubKey());

            if (ipList.size() == 0) {
                break;
            }

            PubkeyIPObject randomNode = Tools.getRandomItemFromList(ipList);
            connectBlocking(randomNode, GET_IPS);
            alreadyFetched.add(randomNode);

            ipList = dbHandler.getIPObjects();
        }

        ipList = dbHandler.getIPObjects();
        System.out.println("fetchNetworkIPs done. Total IPs: " + ipList.size());

    }

    private void startBuildingChannel () throws InterruptedException {
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
    }

    private void connect (PubkeyIPObject ipObject, ChannelIntent intent) throws Exception {
        P2PClient client = new P2PClient(contextFactory);
        Node node = ipObjectToNode(ipObject, intent);
        client.connectTo(node);
    }

    private void connectBlocking (PubkeyIPObject ipObject, ChannelIntent intent) throws Exception {
        P2PClient client = new P2PClient(contextFactory);
        Node node = ipObjectToNode(ipObject, intent);
        client.connectBlocking(node);
    }

    private Node ipObjectToNode (PubkeyIPObject ipObject, ChannelIntent intent) {
        Node node = new Node(this.node);
        node.isServer = false;
        node.intent = intent;
        node.pubKeyClient = ECKey.fromPublicOnly(ipObject.pubkey);
        node.host = ipObject.IP;
        node.port = ipObject.port;
        return node;
    }

}
