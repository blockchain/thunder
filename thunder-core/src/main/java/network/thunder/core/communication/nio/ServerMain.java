package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.p2p.PubkeyIPObject;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class ServerMain {

    public static void main (String[] args) throws Exception {
        P2PContext context = new P2PContext(8992);

        PubkeyIPObject ip = new PubkeyIPObject();
        ip.IP = "157.1.1.5";
        ip.port = 8993;

        context.IPList.add(ip);

        context.startConnections();
    }
}
