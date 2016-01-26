package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.messages.impl.factories.ContextFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.database.DBHandler;
import network.thunder.core.mesh.Node;
import org.bitcoinj.core.ECKey;

import java.util.List;

import static network.thunder.core.communication.processor.ChannelIntent.GET_IPS;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public class ConnectionManagerImpl implements ConnectionManager {
    public final static int NODES_TO_SYNC = 5;
    public final static int CHANNELS_TO_OPEN = 5;
    public final static int MINIMUM_AMOUNT_OF_IPS = 10;

    int port;
    String hostName;
    ECKey keyServer;

    ContextFactory contextFactory;
    DBHandler dbHandler;

    P2PServer server;

    public ConnectionManagerImpl (int port, String hostName) {
        this.port = port;
        this.hostName = hostName;

        contextFactory = new ContextFactoryImpl();
    }

    @Override
    public void startUp () throws Exception {
        startListening();
        connectOpenChannels();

    }

    @Override
    public int getPort () {
        return port;
    }

    @Override
    public String getHostname () {
        return hostName;
    }

    private void startListening () throws Exception {
        server = new P2PServer(contextFactory);
        server.startServer(port);
    }

    private void connectOpenChannels () {
        //TODO
    }

    private void fetchNetworkIPs () throws Exception {
        List<PubkeyIPObject> ipList = dbHandler.getIPObjects();
        if (ipList.size() < MINIMUM_AMOUNT_OF_IPS) {
            List<PubkeyIPObject> seedNodes = SeedNodes.getSeedNodes();
            for (PubkeyIPObject node : seedNodes) {
                connect(node, GET_IPS);
            }
        }
    }

    private void connect (PubkeyIPObject ipObject, ChannelIntent intent) throws Exception {
        P2PClient client = new P2PClient(contextFactory);

        Node node = new Node();
        node.init();
        node.isServer = false;
        node.intent = intent;
        node.pubKeyServer = keyServer;
        node.pubKeyClient = ECKey.fromPublicOnly(ipObject.pubkey);
        node.host = ipObject.IP;
        node.port = ipObject.port;

        client.connectTo(node);

    }

}
