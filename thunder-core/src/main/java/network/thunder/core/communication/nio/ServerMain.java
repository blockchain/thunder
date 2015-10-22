package network.thunder.core.communication.nio;

import network.thunder.core.communication.objects.p2p.PubkeyIPObject;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class ServerMain {

    public static void main (String[] args) throws Exception {

        P2PContext context = new P2PContext(8993);

        PubkeyIPObject ip = new PubkeyIPObject();
        ip.IP = "127.0.0.1";
        ip.port = 8992;
        context.IPList.add(ip);

        PubkeyIPObject ip2 = new PubkeyIPObject();
        ip2.IP = "127.0.0.1";
        ip2.port = 8993;
        context.IPList.add(ip2);
        context.startConnections();

    }
}
