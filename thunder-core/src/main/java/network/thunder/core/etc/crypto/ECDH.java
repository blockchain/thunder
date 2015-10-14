package network.thunder.core.etc.crypto;

import org.bitcoinj.core.ECKey;
import sun.security.ec.ECPrivateKeyImpl;
import sun.security.ec.ECPublicKeyImpl;

import javax.crypto.KeyAgreement;
import java.security.*;

public class ECDH {

	public static byte[] getSharedSecret (ECKey keyServer, ECKey keyClient) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		KeyAgreement aKeyAgree = KeyAgreement.getInstance("ECDH", "BC");

		aKeyAgree.init(new ECPrivateKeyImpl(keyServer.getPrivKeyBytes()));

		aKeyAgree.doPhase(new ECPublicKeyImpl(keyClient.getPubKey()), true);

		MessageDigest hash = MessageDigest.getInstance("SHA1", "BC");

		return hash.digest();
	}

}
