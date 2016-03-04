package network.thunder.core.mesh;

import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.communication.processor.ConnectionResult;
import network.thunder.core.etc.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NodeClient {

    public ECKey pubKeyClient;

    public ECKey ephemeralKeyServer;
    public ECKey ephemeralKeyClient;

    public boolean isServer;

    public ECDHKeySet ecdhKeySet;

    public ChannelIntent intent = ChannelIntent.MISC;
    public ConnectionResult result = ConnectionResult.UNKNOWN;

    public String host;
    public int port;

    public String name;

    public boolean isConnected;

    public NodeClient (String host, int port) {
        this.host = host;
        this.port = port;
        init();
    }

    public NodeClient (ResultSet set) throws SQLException {
        this.host = set.getString("host");
        this.port = set.getInt("port");
        init();
    }

    public NodeClient (NodeClient node) {
        init();
        this.port = node.port;
        this.host = node.host;
        this.pubKeyClient = node.pubKeyClient;
        this.isServer = node.isServer;
        this.intent = node.intent;
        this.name = node.name;
    }

    public NodeClient () {
        init();
    }

    public NodeClient (NodeServer node) {
        init();
        this.host = node.hostServer;
        this.port = node.portServer;
        this.pubKeyClient = node.pubKeyServer;
        this.isServer = false;
        this.name = node.name;
    }

    public void init () {
        pubKeyClient = new ECKey();
        ephemeralKeyServer = new ECKey();
    }

    public interface OnConnectionCloseListener {
        void onClose ();
    }

    @Override
    public String toString () {
        return "NodeClient{" +
                "isConnected=" + isConnected +
                ", name='" + name + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", intent=" + intent +
                ", ecdhKeySet=" + ecdhKeySet +
                ", isServer=" + isServer +
                '}';
    }
}
