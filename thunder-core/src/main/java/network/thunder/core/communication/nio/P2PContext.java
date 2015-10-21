package network.thunder.core.communication.nio;

import network.thunder.core.communication.Node;
import network.thunder.core.communication.objects.p2p.PubkeyChannelObject;
import network.thunder.core.communication.objects.p2p.PubkeyIPObject;
import network.thunder.core.database.DatabaseHandler;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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

    P2PServer server;
    P2PClient client;

    public P2PContext (int port) {
        IPList = new ArrayList<>();
        activeNodes = new ArrayList<>();
        this.port = port;
    }

    public P2PContext (DataSource dataSource) throws Exception {
        IPList = DatabaseHandler.getIPAddresses(dataSource.getConnection());
        activeNodes = DatabaseHandler.getNodesWithOpenChanels(dataSource.getConnection());
    }

    public void startConnections () throws Exception {
        //Open up for incoming connections

        for (Node node : activeNodes) {
            new P2PClient(this).connectTo(node);
            connectedNodes.add(node);
        }

        server = new P2PServer(this);
        server.startServer(port, connectedNodes);
    }

    public void newIPList (ArrayList<PubkeyIPObject> newList) {
        boolean newPubKey = true;
        for (PubkeyIPObject newIP : newList) {
            for (PubkeyIPObject oldIP : IPList) {
                if (Arrays.equals(newIP.pubKey, oldIP.pubKey)) {
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

    public void newIPList (PubkeyIPObject newIP) {
        boolean newPubKey = true;
        for (PubkeyIPObject oldIP : IPList) {
            if (Arrays.equals(newIP.pubKey, oldIP.pubKey)) {
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

    public ArrayList<PubkeyIPObject> getIPList () {
        return IPList;
    }
}
