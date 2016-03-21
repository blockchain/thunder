package network.thunder.core.communication;

import org.bitcoinj.core.ECKey;

public class ServerObject {
    public ECKey pubKeyServer;

    public String hostServer;
    public int portServer;

    public String name;

    public LNConfiguration configuration = new LNConfiguration();

    public ServerObject (ServerObject node) {
        init();
        this.portServer = node.portServer;
        this.hostServer = node.hostServer;
        this.pubKeyServer = node.pubKeyServer;
        this.name = node.name;
    }

    public ServerObject (ClientObject node) {
        init();
        this.portServer = node.port;
        this.hostServer = node.host;
        this.pubKeyServer = node.pubKeyClient;
        this.name = node.name;
    }

    public ServerObject () {
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

        ServerObject that = (ServerObject) o;

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
