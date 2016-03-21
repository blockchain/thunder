package network.thunder.core.helper.crypto;

import org.bitcoinj.core.ECKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEECPrivateKey;
import org.bouncycastle.jce.provider.JCEECPublicKey;

import javax.crypto.KeyAgreement;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;

public class ECDH {

    /*
     * Quite some mess here to have all objects with the correct types...
     */
    public static ECDHKeySet getSharedSecret (ECKey keyServer, ECKey keyClient) {
        try {

            Security.addProvider(new BouncyCastleProvider());
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", "SunEC");
            parameters.init(new ECGenParameterSpec("secp256k1"));
            ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
            ECPrivateKeySpec specPrivate = new ECPrivateKeySpec(keyServer.getPrivKey(), ecParameters);
            ECPublicKeySpec specPublic = new ECPublicKeySpec(new ECPoint(keyClient.getPubKeyPoint().getXCoord().toBigInteger(), keyClient.getPubKeyPoint()
                    .getYCoord().toBigInteger()), ecParameters);

            KeyFactory kf = KeyFactory.getInstance("EC");
            ECPrivateKey privateKey = (ECPrivateKey) kf.generatePrivate(specPrivate);
            ECPublicKey publicKey = (ECPublicKey) kf.generatePublic(specPublic);

            JCEECPrivateKey ecPrivKey = new JCEECPrivateKey(privateKey);
            JCEECPublicKey ecPubKey = new JCEECPublicKey(publicKey);

            new ECKey().getKeyCrypter();

            KeyAgreement aKeyAgree = KeyAgreement.getInstance("ECDH");

            aKeyAgree.init(ecPrivKey);
            aKeyAgree.doPhase(ecPubKey, true);

            return new ECDHKeySet(aKeyAgree.generateSecret(), keyServer.getPubKey(), keyClient.getPubKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//		MessageDigest hash = MessageDigest.getInstance("SHA1", "BC");
//
//		return hash.digest();
    }

}
