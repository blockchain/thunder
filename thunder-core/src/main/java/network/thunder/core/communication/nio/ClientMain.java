package network.thunder.core.communication.nio;

import network.thunder.core.mesh.Node;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class ClientMain {

    public static void main (String[] args) throws Exception {
        P2PContext context = new P2PContext(8993);

        ECKey key = ECKey.fromPrivate(BigInteger.ONE.multiply(BigInteger.valueOf(100000)));
        context.nodeKey = key;

        Node node = new Node();
        node.setHost("127.0.0.1");
        node.setPort(8992);

        context.activeNodes.add(node);
        context.startInitialSyncing();
//        context.startConnections();
    }
}
