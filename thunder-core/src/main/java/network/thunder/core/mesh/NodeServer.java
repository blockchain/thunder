package network.thunder.core.mesh;

import org.bitcoinj.core.ECKey;

public class NodeServer {
    public ECKey pubKeyServer;

    public String hostServer;
    public int portServer;

    public String name;

    public LNConfiguration configuration = new LNConfiguration();

    public NodeServer (NodeServer node) {
        init();
        this.portServer = node.portServer;
        this.hostServer = node.hostServer;
        this.pubKeyServer = node.pubKeyServer;
        this.name = node.name;
    }

    public NodeServer (NodeClient node) {
        init();
        this.portServer = node.port;
        this.hostServer = node.host;
        this.pubKeyServer = node.pubKeyClient;
        this.name = node.name;
    }

    public NodeServer () {
        init();
    }

    public void init () {
        pubKeyServer = new ECKey();
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NodeServer that = (NodeServer) o;

        return pubKeyServer != null ? pubKeyServer.equals(that.pubKeyServer) : that.pubKeyServer == null;

    }

    @Override
    public int hashCode () {
        return pubKeyServer != null ? pubKeyServer.hashCode() : 0;
    }

    public interface OnConnectionCloseListener {
        void onClose ();
    }
}
