package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public class SeedNodes {
    static List<PubkeyIPObject> ipList = new ArrayList<>();

    static {
        PubkeyIPObject seed1 = new PubkeyIPObject();
        seed1.IP = "127.0.0.1";
        seed1.port = 8992;
        ipList.add(seed1);
    }

    public static List<PubkeyIPObject> getSeedNodes () {
        return ipList;
    }
}
