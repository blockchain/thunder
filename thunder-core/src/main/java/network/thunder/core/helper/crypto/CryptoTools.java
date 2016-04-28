package network.thunder.core.helper.crypto;

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

public class CryptoTools {

    public static byte[] addHMAC (byte[] data, byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        byte[] result = mac.doFinal(data);

        byte[] total = new byte[result.length + data.length];
        System.arraycopy(result, 0, total, 0, result.length);
        System.arraycopy(data, 0, total, result.length, data.length);

        return total;
    }

    public static byte[] getHMAC (byte[] data, byte[] keyBytes) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(keySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void checkHMAC (byte[] hmac, byte[] rest, byte[] keyBytes) {
        try {

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(keySpec);
            byte[] result = mac.doFinal(rest);

            if (!Arrays.equals(result, hmac)) {
                throw new RuntimeException("HMAC does not match..");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] createSignature (ECKey pubkey, byte[] data) throws NoSuchProviderException, NoSuchAlgorithmException {

        return pubkey.sign(Sha256Hash.of(data)).encodeToDER();
    }

    public static byte[] decryptAES_CTR (byte[] data, byte[] keyBytes, byte[] ivBytes, long counter) {
        try {
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

            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptAES_CTR (byte[] data, byte[] keyBytes, byte[] ivBytes, long counter) {

        try {
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
            return cipher.doFinal(data);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifySignature (ECKey pubkey, byte[] data, byte[] signature) throws NoSuchProviderException, NoSuchAlgorithmException {
        MessageDigest hashHandler = MessageDigest.getInstance("SHA256", "BC");
        hashHandler.update(data);
        byte[] hash = hashHandler.digest();
        return pubkey.verify(hash, signature);
    }

    public static ECKey getEphemeralKey () {
        return new ECKey();
    }
}
