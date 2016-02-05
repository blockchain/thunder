package network.thunder.core.communication.nio;

import network.thunder.core.etc.ConnectionManagerWrapper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.InMemoryDBHandlerMock;
import network.thunder.core.etc.MockWallet;
import network.thunder.core.mesh.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by matsjerratsch on 26/01/2016.
 */
public class ConnectionManagerImplTest {

    final static int AMOUNT_OF_NODES = 100;
    final static int PORT_START = 20000;

    ConnectionManagerWrapper connectionSeed;

    List<ConnectionManagerWrapper> clients = new ArrayList<>();

    @Before
    public void prepare () {
        createSeedConnection();
        createNodes();
    }

    @Test
    public void test () throws Exception {
        Logger.getLogger("io.netty").setLevel(Level.OFF);

        connectionSeed.connectionManager.startListening();

//        clients.get(0).connectionManager.startUp();
//        for(int i = 1; i<5; ++i) {
//
//            System.out.println("------------------------------------------------------------------------------");
//            System.out.println(i + " started up...");
//
//
//            clients.get(i).connectionManager.startUp();
//            Thread.sleep(1000);
//        }
//
        for (int i = 6; i < 10; ++i) {

            System.out.println("------------------------------------------------------------------------------");
            System.out.println(i + " started up...");

            clients.get(i).connectionManager.startUp();
            Thread.sleep(500);
        }
//        }
//        Thread.sleep(5000);
//        clients.get(1).connectionManager.startUp();
//
//        Thread.sleep(2000);
//
//        clients.get(2).connectionManager.startUp();
//
//        Thread.sleep(2000);
//
//        clients.get(3).connectionManager.startUp();

        Thread.sleep(1000);
    }

    private void createSeedConnection () {
        SeedNodes.setToTestValues();
        Node node = SeedNodes.nodeList.get(0);

        connectionSeed = getConnection(node);
    }

    private void createNodes () {
        for (int i = 0; i < AMOUNT_OF_NODES; ++i) {
            int port = PORT_START + i;
            String host = "127.0.0.1";

            Node node = new Node();
            node.init();
            node.portServer = port;
            node.hostServer = host;

            ConnectionManagerWrapper wrapper = getConnection(node);

            clients.add(wrapper);
        }
    }

    private ConnectionManagerWrapper getConnection (Node node) {
        ConnectionManagerWrapper connection = new ConnectionManagerWrapper();
        connection.dbHandler = new InMemoryDBHandlerMock();
        connection.wallet = new MockWallet(Constants.getNetwork());
        connection.connectionManager = new ConnectionManagerImpl(node, connection.wallet, connection.dbHandler);
        return connection;
    }

}