package network.thunder.core.communication.objects.p2p;

import network.thunder.core.etc.Tools;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class PubkeyIPObject {
	public String IP;
	public int port;
	public byte[] pubKey;
	public byte[] signature;
	public int timestamp;

	public PubkeyIPObject () {}

	public PubkeyIPObject (ResultSet set) throws SQLException {
		this.IP = set.getString("address");
		this.port = set.getInt("port");
		this.timestamp = set.getInt("timestamp");
		this.signature = set.getBytes("signature");
		this.pubKey = set.getBytes("pubkey");
	}

	public void verify () {
		//TODO: Implement signature verification..
	}

	public String getHash () {
		return Tools.bytesToHex(Tools.hashSecret(getData()));
	}

	private byte[] getData () {
		//TODO: Have some proper summary here..
		return signature;
	}
}
