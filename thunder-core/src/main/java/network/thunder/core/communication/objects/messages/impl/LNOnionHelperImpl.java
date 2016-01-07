package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentOnionMessageEncrypted;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.etc.crypto.CryptoTools;
import network.thunder.core.etc.crypto.ECDH;
import network.thunder.core.etc.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public class LNOnionHelperImpl implements LNOnionHelper {
    private final static int BYTES_OFFSET_PER_ENCRYPTION = 150;

    ECKey keyServer;

    LNPaymentOnionMessageEncrypted encryptedOnionObject;
    ECKey receivedHop;

    ECDHKeySet keySet;

    byte[] decryptedData;
    ECKey ephemeralKey;
    ECKey nextHop;

    byte[] encryptedDataForNextHop;



    @Override
    public void init (ECKey keyServer) {
        this.keyServer = keyServer;
    }

    @Override
    public void loadMessage (ECKey receivedHop, LNPaymentOnionMessageEncrypted encryptedOnionObject) {
        this.encryptedOnionObject = encryptedOnionObject;
        this.receivedHop = receivedHop;
        decryptMessage();
        parseMessage();
    }

    @Override
    public ECKey getNextHop () {
        return nextHop;
    }

    @Override
    public LNPaymentOnionMessageEncrypted getMessageForNextHop () {
        return null;
    }

    void decryptMessage () {
        ephemeralKey = ECKey.fromPublicOnly(encryptedOnionObject.ephemeralKey);
        keySet = ECDH.getSharedSecret(keyServer, ephemeralKey);

        CryptoTools.checkHMAC(encryptedOnionObject.hmac, encryptedOnionObject.data, keySet.hmacKey);

        decryptedData = CryptoTools.decryptAES_CTR(encryptedOnionObject.data, keySet.encryptionKey, keySet.ivServer, 0);
    }

    void parseMessage() {
        byte[] pubkeyOfNextHop = new byte[33];
        System.arraycopy(decryptedData, 0, pubkeyOfNextHop, 0, 33);

        nextHop = ECKey.fromPublicOnly(pubkeyOfNextHop);


        encryptedDataForNextHop = new byte[decryptedData.length-BYTES_OFFSET_PER_ENCRYPTION];
    }
}
