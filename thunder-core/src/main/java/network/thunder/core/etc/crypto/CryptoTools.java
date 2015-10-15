package network.thunder.core.etc.crypto;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

/**
 * Created by matsjerratsch on 12/10/2015.
 */
public class CryptoTools {

	public static byte[] decryptAES_CTR (byte[] data, byte[] keyBytes, byte[] ivBytes, long counter) {

		try {

//			byte[] keyBytes = new byte[]{(byte) 0x36, (byte) 0xf1, (byte) 0x83, (byte) 0x57, (byte) 0xbe, (byte) 0x4d, (byte) 0xbd, (byte) 0x77, (byte) 0xf0,
//					(byte) 0x50, (byte) 0x51, (byte) 0x5c, 0x73, (byte) 0xfc, (byte) 0xf9, (byte) 0xf2};

//			byte[] ivBytes = new byte[]{(byte) 0x69, (byte) 0xdd, (byte) 0xa8, (byte) 0x45, (byte) 0x5c, (byte) 0x7d, (byte) 0xd4, (byte) 0x25, (byte) 0x4b,
//					(byte) 0xf3, (byte) 0x53, (byte) 0xb7, (byte) 0x73, (byte) 0x30, (byte) 0x4e, (byte) 0xec};

			byte[] ivWithCounter = new byte[16];
			System.arraycopy(ivBytes, 0, ivWithCounter, 0, ivBytes.length);
			byte[] counterBytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(counter).array();
			System.arraycopy(counterBytes, 0, ivWithCounter, ivBytes.length, counterBytes.length);

			//Initialisation
			SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(ivWithCounter);

			//Mode
			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			byte[] plain = cipher.doFinal(data);

			String plaintext = new String(plain);
			System.out.println("plaintext: " + plaintext);

			return plain;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] encryptAES_CTR (byte[] data, byte[] keyBytes, byte[] ivBytes, long counter) {

		try {

//			byte[] keyBytes = new byte[]{(byte) 0x36, (byte) 0xf1, (byte) 0x83, (byte) 0x57, (byte) 0xbe, (byte) 0x4d, (byte) 0xbd, (byte) 0x77, (byte) 0xf0,
//					(byte) 0x50, (byte) 0x51, (byte) 0x5c, 0x73, (byte) 0xfc, (byte) 0xf9, (byte) 0xf2};

//			byte[] ivBytes = new byte[]{(byte) 0x69, (byte) 0xdd, (byte) 0xa8, (byte) 0x45, (byte) 0x5c, (byte) 0x7d, (byte) 0xd4, (byte) 0x25, (byte) 0x4b,
//					(byte) 0xf3, (byte) 0x53, (byte) 0xb7, (byte) 0x73, (byte) 0x30, (byte) 0x4e, (byte) 0xec};

			byte[] ivWithCounter = new byte[16];
			System.arraycopy(ivBytes, 0, ivWithCounter, 0, ivBytes.length);
			byte[] counterBytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(counter).array();
			System.arraycopy(counterBytes, 0, ivWithCounter, ivBytes.length, counterBytes.length);

			//Initialisation
			System.out.println(keyBytes.length);
			SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(ivWithCounter);

			System.out.println(key);

			//Mode
			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			byte[] original = cipher.doFinal(data);

			String plaintext = new String(original);
			System.out.println("plaintext: " + plaintext);

			return original;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] addHMAC (byte[] data, byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeyException {
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(keySpec);
		byte[] result = mac.doFinal(data);

		byte[] total = new byte[result.length+data.length];
		System.arraycopy(result, 0, total, 0, result.length);
		System.arraycopy(data, 0, total, result.length, data.length);


		return total;
	}

	public static byte[] checkAndRemoveHMAC (byte[] data, byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeyException {
		byte[] hmac = new byte[20];
		byte[] rest = new byte[data.length-hmac.length];
		System.arraycopy(data, 0, hmac, 0, hmac.length);
		System.arraycopy(data, hmac.length, rest, 0, data.length-hmac.length);


		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(keySpec);
		byte[] result = mac.doFinal(rest);

		if(Arrays.equals(result, hmac)) {
			return rest;
		}


		throw new RuntimeException("HMAC does not match..");



	}

	public static void verifySignature(ECKey pubkey, byte[] data, byte[] signature) throws NoSuchProviderException, NoSuchAlgorithmException {
		MessageDigest hashHandler = MessageDigest.getInstance("SHA256", "BC");
		hashHandler.update(data);
		byte[] hash = hashHandler.digest();

		if(!pubkey.verify(hash, signature)) {
			System.out.println("Signature does not match..");
			throw new RuntimeException("Signature does not match..");
		}
	}

	public static byte[] createSignature(ECKey pubkey, byte[] data) throws NoSuchProviderException, NoSuchAlgorithmException {

		return pubkey.sign(Sha256Hash.of(data)).encodeToDER();
	}
}
