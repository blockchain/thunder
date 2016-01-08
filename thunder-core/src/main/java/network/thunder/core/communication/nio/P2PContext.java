package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyChannelObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.database.DatabaseHandler;
import network.thunder.core.mesh.Node;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Wallet;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by matsjerratsch on 14/10/2015.
 */
public class P2PContext {

    public static final String HOST = "127.0.0.1";
    public int port = 8892;

    public ArrayList<Node> connectedNodes = new ArrayList<>();
    public ArrayList<PubkeyIPObject> IPList;
    public ArrayList<Node> activeNodes;

    public HashMap<String, PubkeyChannelObject> pubkeyChannelObjectHashMap = new HashMap<>();
    public ECKey nodeKey;
    public boolean fetchFreshIPs = true;
//    public SynchronizationHelper syncDatastructure = new SynchronizationHelper();
    public boolean needsInitialSyncing = false;
    public DataSource dataSource;

    //Balance available for new channels
    public long balance = 10000000000000L;
    public Wallet wallet;
    public HashMap<TransactionOutPoint, Integer> lockedOutputs = new HashMap<>();

    P2PServer server;
    boolean keepReconnectingToNewNodes = false;
    P2PContext context;

    public P2PContext (int port) {
        IPList = new ArrayList<>();
        activeNodes = new ArrayList<>();
        this.port = port;
        context = this;

        try {
            nodeKey = new ECKey(SecureRandom.getInstanceStrong());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            dataSource = DatabaseHandler.getDataSource();
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    public P2PContext (DataSource dataSource) throws Exception {
        IPList = DatabaseHandler.getIPAddresses(dataSource.getConnection());
        activeNodes = DatabaseHandler.getNodesWithOpenChanels(dataSource.getConnection());
    }

    public ArrayList<PubkeyIPObject> getIPList () {
        return IPList;
    }

    public void newIP (PubkeyIPObject newIP) {
        boolean newPubKey = true;
        for (PubkeyIPObject oldIP : IPList) {
            if (Arrays.equals(newIP.pubkey, oldIP.pubkey)) {
                newPubKey = false;
                if (newIP.timestamp > oldIP.timestamp) {
                    //Got a new IP address for an existing pubkey
                } else {
                    //Our IP address is newer than his one?
                    //TODO: Send the more recent IP address back?
                }
            }
        }
//        if (newPubKey) {
        //TODO: Got a pubkey we don't know about yet. Should probably do some checking here
        IPList.add(newIP);
//        }

    }

    public void newIPList (ArrayList<PubkeyIPObject> newList) {
        boolean newPubKey = true;
        for (PubkeyIPObject newIP : newList) {
            for (PubkeyIPObject oldIP : IPList) {
                if (Arrays.equals(newIP.pubkey, oldIP.pubkey)) {
                    newPubKey = false;
                    if (newIP.timestamp > oldIP.timestamp) {
                        //Got a new IP address for an existing pubkey
                    } else {
                        //Our IP address is newer than his one?
                        //TODO: Send the more recent IP address back?
                    }
                }
            }
            if (newPubKey) {
                //TODO: Got a pubkey we don't know about yet. Should probably do some checking here
                IPList.add(newIP);
            }
        }
    }

    public void startConnectingToRandomNodes () {
        keepReconnectingToNewNodes = true;
        new Thread(new Runnable() {
            @Override
            public void run () {
//                while (!syncDatastructure.fullySynchronized()) {

                    try {
                        if (connectedNodes.size() < 10) {
                            if (IPList.size() > 0) {
                                PubkeyIPObject ipObject = IPList.get(new Random().nextInt(IPList.size()));
                                Node node = new Node(ipObject.IP, ipObject.port);

                                new P2PClient(context).connectTo(node);
                                IPList.remove(ipObject); //TODO: Maybe not the best to just delete it here?
                            }
                        }

                        Thread.sleep(5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                }

                stopConnectingToRandomNodes();
            }
        }).start();
    }

    public void startConnections () throws Exception {
        //Open up for incoming connections

        for (Node node : activeNodes) {
            new P2PClient(this).connectTo(node);
        }

        server = new P2PServer(this);
        server.startServer(port, connectedNodes);
    }

    public void startInitialSyncing () {
        //TODO: Have some initial bootstrap node hardcoded here..
        Node initialNode = new Node("127.0.0.1", 8992);
        initialNode.justFetchNewIpAddresses = true;

        try {
            new P2PClient(this).connectTo(initialNode);

            initialNode.setOnConnectionCloseListener(new Node.OnConnectionCloseListener() {
                @Override
                public void onClose () {
                    System.out.println("Initial Node IP getting done..");
                    startConnectingToRandomNodes();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stopConnectingToRandomNodes () {
        keepReconnectingToNewNodes = false;
        //Drop all connections..
        for (Node node : connectedNodes) {
            node.getNettyContext().close();
        }

        System.out.println("Received all sync data...");
//        syncDatastructure.saveFullSyncToDatabase();

    }

    public HashMap<TransactionOutPoint, Integer> getLockedOutputs () {
        return lockedOutputs;
    }

    public Wallet getWallet () {
        return wallet;
    }

    public long getAmountForNewChannel () {
        return balance / 10;
    }
}
