package network.thunder.core.communication;

import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.helper.callback.ResultCommand;
import network.thunder.core.helper.callback.results.NullResultCommand;
import network.thunder.core.helper.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientObject {
    public boolean isServer;

    //TODO transition to NodeKey.class
    public ECKey pubKeyClient;

    //Encryption keys
    public ECKey ephemeralKeyServer;
    public ECKey ephemeralKeyClient;
    public ECDHKeySet ecdhKeySet;

    public ConnectionIntent intent = ConnectionIntent.MISC;
    public ResultCommand resultCallback = new NullResultCommand();

    public String host;
    public int port;

    public String name;

    public ClientObject (String host, int port) {
        this.host = host;
        this.port = port;
        init();
    }

    public ClientObject (ResultSet set) throws SQLException {
        this.host = set.getString("host");
        this.port = set.getInt("port");
        init();
    }

    public ClientObject (ClientObject node) {
        init();
        this.port = node.port;
        this.host = node.host;
        this.pubKeyClient = node.pubKeyClient;
        this.isServer = node.isServer;
        this.intent = node.intent;
        this.name = node.name;
    }

    public ClientObject () {
        init();
    }

    public ClientObject (ServerObject node) {
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
        return "ClientObject{" +
                ", name='" + name + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", intent=" + intent +
                ", ecdhKeySet=" + ecdhKeySet +
                ", isServer=" + isServer +
                '}';
    }
}
