package network.thunder.core.communication;

import network.thunder.core.etc.NodeWrapper;
import network.thunder.core.etc.SeedNodes;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.results.NullResultCommand;
import org.bitcoinj.core.ECKey;
import org.junit.Before;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class FullCommTest {

    public static final int NUMBER_NODES = 9;
    public static final int NUMBER_CHANNELS = 5;

    @Before
    public void init () {

    }

    /**
     * Complete integration test. Normally not good practice to test for so many different failures within one test, but
     * because of timing we would otherwise massively increase the time to run the test suite.
     * <p>
     * For now this seems to be a good trade-off, maybe mocking the Netty interface can cut down the time to run the tests later.
     * <p>
     * Because this tests actually sends messages over open connections and these are treated as file descriptors under
     * unix systems, we have to limit the amount of nodes to 9 for now.
     */
    //TODO Add payments to the test
    //TODO Add syncing to the test
//    @Test
    public void totalTest () throws InterruptedException {
        List<NodeWrapper> nodeList = setupNodes();
        setupSeedNode(nodeList);
        seedNodes(nodeList);
        Thread.sleep(2500);
        seedNodes(nodeList);
        Thread.sleep(2500);
        for (NodeWrapper nodeWrapper1 : nodeList) {
            for (NodeWrapper nodeWrapper2 : nodeList) {
                if (!nodeWrapper1.dbHandler.getIPObjects().stream().anyMatch(s -> s.port == nodeWrapper2.serverObject.portServer)) {
                    if (nodeWrapper1.serverObject.portServer != nodeWrapper2.serverObject.portServer) {
                        throw new RuntimeException(
                                "Missing IP object in list from " + nodeWrapper1.serverObject.portServer + ": " + nodeWrapper2.serverObject.portServer);
                    }
                }
            }
        }

        buildPaymentChannels(nodeList);
    }

    private static void buildPaymentChannels (List<NodeWrapper> nodeList) throws InterruptedException {
        Map<NodeWrapper, List<ByteBuffer>> shouldConnect = new HashMap<>();
        for (NodeWrapper nodeWrapper : nodeList) {
            shouldConnect.put(nodeWrapper, new ArrayList<>());
            List<byte[]> nodes = nodeList.stream()
                    .map(n -> n.serverObject.pubKeyServer.getPubKey())
                    .filter(n -> !Arrays.equals(n, nodeWrapper.serverObject.pubKeyServer.getPubKey()))
                    .collect(Collectors.toList());
            for (int i = 0; i < NUMBER_CHANNELS; i++) {
                byte[] node = Tools.getRandomItemFromList(nodes);
                nodes = nodes.stream().filter(n -> !Arrays.equals(n, node)).collect(Collectors.toList());

                shouldConnect.get(nodeWrapper).add(ByteBuffer.wrap(node));
                nodeWrapper.thunderContext.openChannel(node, new NullResultCommand());
                Thread.sleep(100);
            }
        }

        Thread.sleep(2000);
        for (NodeWrapper nodeWrapper : nodeList) {
            List<ByteBuffer> shouldConnected = shouldConnect.get(nodeWrapper);
            List<ByteBuffer> actuallyConnected = new ArrayList<>();
            shouldConnected.stream()
                    .map(ByteBuffer::array)
                    .map(ECKey::fromPublicOnly)
                    .map(NodeKey::new)
                    .forEach(pubkey -> actuallyConnected.addAll(
                            nodeWrapper.dbHandler.getOpenChannel(pubkey).
                                    stream()
                                    .map(p -> p.nodeKeyClient.getPubKey())
                                    .map(ByteBuffer::wrap)
                                    .collect(Collectors.toList())));

            if (!actuallyConnected.containsAll(shouldConnected)) {
                throw new RuntimeException(nodeWrapper.serverObject.portServer + " did not built channels correctly.");

            }
        }
    }

    private static void makePayments (List<NodeWrapper> nodeList) {
        for (NodeWrapper nodeWrapper : nodeList) {

        }
    }

    private static void seedNodes (List<NodeWrapper> nodeList) throws InterruptedException {
        for (NodeWrapper nodeWrapper : nodeList) {
            nodeWrapper.thunderContext.fetchNetworkIPs(new NullResultCommand());
            Thread.sleep(50);
        }
    }

    private static void setupSeedNode (List<NodeWrapper> nodeList) {
        NodeWrapper seedNode = Tools.getRandomItemFromList(nodeList);
        SeedNodes.ipList.clear();
        SeedNodes.ipList.add(
                SeedNodes.createIPObject(
                        "localhost",
                        seedNode.serverObject.pubKeyServer.getPublicKeyAsHex(),
                        seedNode.serverObject.portServer));
    }

    public static List<NodeWrapper> setupNodes () {
        List<NodeWrapper> nodeList = new ArrayList<>();
        for (int i = 0; i < NUMBER_NODES; i++) {
            NodeWrapper nodeWrapper = new NodeWrapper();
            nodeWrapper.init(10000 + i);
            nodeWrapper.thunderContext.startListening(new NullResultCommand());
            nodeList.add(nodeWrapper);
        }
        return nodeList;
    }
}
