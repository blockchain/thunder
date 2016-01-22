package network.thunder.core.mesh;

import io.netty.channel.ChannelHandlerContext;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.processor.ChannelIntent;
import network.thunder.core.etc.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class Node {

    public static final int THRESHHOLD_INVENTORY_AMOUNT_TO_SEND = 32;
    //From the gossip handler upwards nodes have their own connection object
    public Connection conn;
    public boolean justFetchNewIpAddresses = false;
    public P2PContext context;
    public ECKey pubKeyTempClient;
    public ECKey pubKeyTempServer;

    public ECKey pubKeyClient;
    public ECKey pubKeyServer;

    public ECKey ephemeralKeyServer;
    public ECKey ephemeralKeyClient;

    public boolean isServer;

    public ECDHKeySet ecdhKeySet;

    public ChannelIntent intent = ChannelIntent.MISC;

    private String host;
    private int port;
    private boolean connected = false;
    private ChannelHandlerContext nettyContext;
    private byte[] pubkey;

    private boolean isReady;
    private boolean hasOpenChannel;
    private ArrayList<byte[]> inventoryList = new ArrayList<>();
    private OnConnectionCloseListener onConnectionCloseListener;

    public String name;

    public Node (String host, int port) {
        this.host = host;
        this.port = port;
        init();
    }

    public Node (ResultSet set) throws SQLException {
        this.host = set.getString("host");
        this.port = set.getInt("port");
        init();
    }

    public Node (byte[] pubkey) {
        this.pubkey = pubkey;
        init();
    }

    public Node () {
        init();
    }

    private void init () {
        ephemeralKeyServer = new ECKey();
        pubKeyServer = new ECKey();
        pubKeyTempServer = new ECKey();

    }

    public void closeConnection () {
        if (onConnectionCloseListener != null) {
            onConnectionCloseListener.onClose();
        }
        try {
            this.nettyContext.close();
        } catch (Exception e) {
        }
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node node = (Node) o;

        return Arrays.equals(pubkey, node.pubkey);

    }

    public String getHost () {
        return host;
    }

    public void setHost (String host) {
        this.host = host;
    }

    public ChannelHandlerContext getNettyContext () {
        return nettyContext;
    }

    public void setNettyContext (ChannelHandlerContext nettyContext) {
        this.nettyContext = nettyContext;
    }

    public int getPort () {
        return port;
    }

    public void setPort (int port) {
        this.port = port;
    }

    @Override
    public int hashCode () {
        return pubkey != null ? Arrays.hashCode(pubkey) : 0;
    }

    public boolean isConnected () {
        return connected;
    }

    public void setConnected (boolean connected) {
        this.connected = connected;
    }

    public void setOnConnectionCloseListener (OnConnectionCloseListener onConnectionCloseListener) {
        this.onConnectionCloseListener = onConnectionCloseListener;
    }

    public interface OnConnectionCloseListener {
        public void onClose ();
    }
}
