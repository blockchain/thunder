package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.NodeClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public class SeedNodes {
    static List<PubkeyIPObject> ipList = new ArrayList<>();
    static List<NodeClient> nodeList = new ArrayList<>();

    static {
        PubkeyIPObject seed1 = new PubkeyIPObject();
        seed1.IP = "127.0.0.1";
        seed1.port = 8992;
        ipList.add(seed1);
    }

    public static List<PubkeyIPObject> getSeedNodes () {
        return ipList;
    }

    public static void setToTestValues () {
        NodeClient node = new NodeClient();
        node.init();
        node.host = "localhost";
        node.port = 10001;

        PubkeyIPObject seed1 = new PubkeyIPObject();
        seed1.IP = "localhost";
        seed1.port = 10001;
        seed1.pubkey = node.pubKeyClient.getPubKey();

        ipList.clear();

        nodeList.add(node);
        ipList.add(seed1);
    }
}
