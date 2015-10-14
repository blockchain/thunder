package network.thunder.core.communication;

import io.netty.channel.ChannelHandlerContext;
import network.thunder.core.communication.objects.subobjects.AuthenticationObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Node {
	private String host;
	private int port;

	private boolean connected = false;

	private ChannelHandlerContext nettyContext;

	private byte[] pubkey;
	private boolean isAuth;
	private boolean sentAuth;
	private boolean authFinished;
	private boolean isReady;
	private boolean hasOpenChannel;

	public Node (String host, int port) {
		this.host = host;
		this.port = port;
	}

	public Node(ResultSet set) throws SQLException {
		this.host = set.getString("host");
		this.port = set.getInt("port");
	}

	public Node () {

	}

	public boolean processAuthentication (AuthenticationObject authentication, byte[] secret) {
		//TODO: Check authentication based on the supplied pubkey

		isAuth = true;
		if (sentAuth) {
			authFinished = true;
		}
		return true;
	}

	public AuthenticationObject getAuthenticationObject (byte[] secret) {
		//TODO: Produce a proper authentication object..

		sentAuth = true;
		return new AuthenticationObject();
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


}
