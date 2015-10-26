package network.thunder.core.mesh;

import io.netty.channel.ChannelHandlerContext;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.objects.subobjects.AuthenticationObject;
import network.thunder.core.etc.crypto.CryptoTools;
import org.bitcoinj.core.ECKey;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class Node {
    private String host;
    private int port;

    private boolean connected = false;

    private ChannelHandlerContext nettyContext;

    private byte[] pubkey;

    private ECKey pubKeyTempClient;
    private ECKey pubKeyTempServer;

    private boolean isAuth;
    private boolean sentAuth;
    private boolean authFinished;
    private boolean isReady;
    private boolean hasOpenChannel;

    //From the gossip handler upwards nodes have their own connection object
    public Connection conn;

    public boolean justFetchNewIpAddresses = false;

    public P2PContext context;

    public Node (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Node (ResultSet set) throws SQLException {
        this.host = set.getString("host");
        this.port = set.getInt("port");
    }

    public Node (byte[] pubkey) {
        this.pubkey = pubkey;
    }

    public Node () {
    }

    public boolean processAuthentication (AuthenticationObject authentication, ECKey pubkeyClient, ECKey pubkeyServerTemp) throws NoSuchProviderException, NoSuchAlgorithmException {

        byte[] data = new byte[pubkeyClient.getPubKey().length + pubkeyServerTemp.getPubKey().length];
        System.arraycopy(pubkeyClient.getPubKey(), 0, data, 0, pubkeyClient.getPubKey().length);
        System.arraycopy(pubkeyServerTemp.getPubKey(), 0, data, pubkeyClient.getPubKey().length, pubkeyServerTemp.getPubKey().length);

        CryptoTools.verifySignature(pubkeyClient, data, authentication.signature);

        isAuth = true;
        if (sentAuth) {
            authFinished = true;
        }
        return true;
    }

    public AuthenticationObject getAuthenticationObject (ECKey keyServer, ECKey keyClientTemp) throws NoSuchProviderException, NoSuchAlgorithmException {

        byte[] data = new byte[keyServer.getPubKey().length + keyClientTemp.getPubKey().length];
        System.arraycopy(keyServer.getPubKey(), 0, data, 0, keyServer.getPubKey().length);
        System.arraycopy(keyClientTemp.getPubKey(), 0, data, keyServer.getPubKey().length, keyClientTemp.getPubKey().length);

        AuthenticationObject obj = new AuthenticationObject();
        obj.pubkeyServer = keyServer.getPubKey();
        obj.signature = CryptoTools.createSignature(keyServer, data);

        sentAuth = true;
        if (this.isAuth) {
            this.authFinished = true;
        }
        return obj;
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

    public String getHost () {
        return host;
    }

    public void setHost (String host) {
        this.host = host;
    }

    public boolean isConnected () {
        return connected;
    }

    public void setConnected (boolean connected) {
        this.connected = connected;
    }

    public boolean hasSentAuth () {
        return sentAuth;
    }

    public boolean isAuth () {
        return isAuth;
    }

    public boolean allowsAuth () {
        return !isAuth;
    }

    public void finishAuth () {
        authFinished = true;
    }

    public boolean isAuthFinished () {
        return authFinished;
    }

    public ECKey getPubKeyTempClient () {
        return pubKeyTempClient;
    }

    public void setPubKeyTempClient (ECKey pubKeyTempClient) {
        this.pubKeyTempClient = pubKeyTempClient;
    }

    public ECKey getPubKeyTempServer () {
        return pubKeyTempServer;
    }

    public void setPubKeyTempServer (ECKey pubKeyTempServer) {
        this.pubKeyTempServer = pubKeyTempServer;
    }

    private OnConnectionCloseListener onConnectionCloseListener;

    public void setOnConnectionCloseListener (OnConnectionCloseListener onConnectionCloseListener) {
        this.onConnectionCloseListener = onConnectionCloseListener;
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

    public interface OnConnectionCloseListener {
        public void onClose ();
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

    @Override
    public int hashCode () {
        return pubkey != null ? Arrays.hashCode(pubkey) : 0;
    }
}
