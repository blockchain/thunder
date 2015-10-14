package network.thunder.core.etc.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by matsjerratsch on 14/10/2015.
 */
public class ECDHKeySet {

	private byte[] masterKey;
	private byte[] encryptionKey;
	private byte[] hmacKey;
	private byte[] ivClient;
	private byte[] ivServer;

	public ECDHKeySet(byte[] masterKey, byte[] serverPubkey, byte[] clientPubkey) throws NoSuchProviderException, NoSuchAlgorithmException {
		this.masterKey = masterKey;
		MessageDigest hash = MessageDigest.getInstance("RIPEMD128", "BC");
		byte[] t = new byte[masterKey.length+1];
		System.arraycopy(masterKey, 0, t, 0, masterKey.length);
		t[t.length-1] = 0x00;

		hash.update(t);

		encryptionKey = hash.digest();

		t[t.length-1] = 0x01;
		hash.update(t);

		hmacKey = hash.digest();

		byte[] a1 = new byte[masterKey.length + serverPubkey.length];
		byte[] a2 = new byte[masterKey.length + serverPubkey.length];

		System.arraycopy(masterKey, 0, a1, 0, masterKey.length);
		System.arraycopy(serverPubkey, 0, a1, masterKey.length, serverPubkey.length);

		System.arraycopy(masterKey, 0, a2, 0, masterKey.length);
		System.arraycopy(clientPubkey, 0, a2, masterKey.length, clientPubkey.length);

		ivClient = new byte[8];
		ivServer = new byte[8];

		hash.update(a1);
		byte[] b1 = hash.digest();
		System.arraycopy(b1, 0, ivServer, 0, 8);

		hash.update(a2);
		byte[] b2 = hash.digest();
		System.arraycopy(b2, 0, ivClient, 0, 8);


//		return hash.digest();
	}

	public byte[] getEncryptionKey () {
		return encryptionKey;
	}

	public byte[] getHmacKey () {
		return hmacKey;
	}

	public byte[] getIvClient () {
		return ivClient;
	}

	public byte[] getIvServer () {
		return ivServer;
	}
}
