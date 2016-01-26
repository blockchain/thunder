package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.etc.Tools;
import network.thunder.core.etc.crypto.CryptoTools;
import network.thunder.core.etc.crypto.ECDH;
import network.thunder.core.etc.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;

import java.util.Arrays;
import java.util.List;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public class LNOnionHelperImpl implements LNOnionHelper {
    private final static int BYTES_OFFSET_PER_ENCRYPTION = 150;

    ECKey keyServer;

    OnionObject encryptedOnionObject;
    ECKey receivedHop;

    ECDHKeySet keySet;

    byte[] decryptedData;
    ECKey ephemeralKey;
    ECKey nextHop;

    byte[] encryptedDataForNextHop;

    boolean lastHopReached = false;

    @Override
    public void init (ECKey keyServer) {
        this.keyServer = keyServer;
    }

    @Override
    public void loadMessage (ECKey receivedHop, OnionObject encryptedOnionObject) {
        this.encryptedOnionObject = encryptedOnionObject;
        this.receivedHop = receivedHop;
        decryptMessage();
        if (!lastHopReached) {
            parseMessage();
        }
    }

    @Override
    public ECKey getNextHop () {
        return nextHop;
    }

    @Override
    public OnionObject getMessageForNextHop () {
        byte[] padding = new byte[OnionObject.TOTAL_LENGTH];

        byte[] paddingEnc = CryptoTools.encryptAES_CTR(padding, keySet.encryptionKey, keySet.ivClient, 0);
        byte[] data = new byte[OnionObject.TOTAL_LENGTH * OnionObject.MAX_HOPS];

        System.arraycopy(decryptedData, OnionObject.DATA_LENGTH, data, 0, decryptedData.length - OnionObject.DATA_LENGTH);
        System.arraycopy(paddingEnc, 0, data, getEncryptedLengthNextHop(), paddingEnc.length);

        return new OnionObject(data);
    }

    public static int getEncryptedLengthNextHop () {
        return (OnionObject.TOTAL_LENGTH * (OnionObject.MAX_HOPS - 1));
    }

    void decryptMessage () {
        byte[] key = new byte[OnionObject.KEY_LENGTH];
        byte[] hmac = new byte[OnionObject.HMAC_LENGTH];
        byte[] data = new byte[OnionObject.TOTAL_LENGTH * OnionObject.MAX_HOPS - (OnionObject.KEY_LENGTH + OnionObject.HMAC_LENGTH)];

        System.arraycopy(encryptedOnionObject.data, 0, key, 0, key.length);
        System.arraycopy(encryptedOnionObject.data, key.length, hmac, 0, hmac.length);
        System.arraycopy(encryptedOnionObject.data, key.length + hmac.length, data, 0, data.length);

        ephemeralKey = ECKey.fromPublicOnly(key);
        keySet = ECDH.getSharedSecret(keyServer, ephemeralKey);
        decryptedData = CryptoTools.decryptAES_CTR(data, keySet.encryptionKey, keySet.ivServer, 0);

        byte[] dataToSign = new byte[OnionObject.DATA_LENGTH];
        System.arraycopy(decryptedData, 0, dataToSign, 0, OnionObject.DATA_LENGTH);

        CryptoTools.checkHMAC(hmac, dataToSign, keySet.hmacKey);
    }

    void parseMessage () {
        byte[] pubkeyOfNextHop = new byte[33];
        System.arraycopy(decryptedData, 0, pubkeyOfNextHop, 0, 33);

        byte[] emptyData = new byte[OnionObject.KEY_LENGTH];
        if (Arrays.equals(emptyData, pubkeyOfNextHop)) {
            System.out.println("We are the last hop..");
            nextHop = keyServer;
            lastHopReached = true;
            return;
        }

        nextHop = ECKey.fromPublicOnly(pubkeyOfNextHop);
    }

    public OnionObject createOnionObject (List<byte[]> nodeList) {
        if (nodeList.size() > OnionObject.MAX_HOPS) {
            throw new RuntimeException("Too many nodes in nodeList");
        }

        int byteCount = OnionObject.MAX_HOPS * OnionObject.TOTAL_LENGTH;
        byte[] data = Tools.getRandomByte(byteCount);

        for (int i = 0; i < nodeList.size(); ++i) {
            byte[] temp = new byte[byteCount];
            byte[] dataToSign = new byte[OnionObject.DATA_LENGTH];
            System.arraycopy(data, 0, temp, OnionObject.DATA_LENGTH, data.length - OnionObject.DATA_LENGTH);

            ECKey key = ECKey.fromPublicOnly(nodeList.get(nodeList.size() - 1 - i));
            ECKey keyServer = CryptoTools.getEphemeralKey();
            ECDHKeySet keySet = ECDH.getSharedSecret(keyServer, key);

            if (i > 0) {
                byte[] nextNode = nodeList.get(nodeList.size() - i);
                System.arraycopy(nextNode, 0, dataToSign, 0, nextNode.length);
            }

            System.arraycopy(dataToSign, 0, temp, 0, dataToSign.length);

            byte[] encryptedTemp = CryptoTools.encryptAES_CTR(temp, keySet.encryptionKey, keySet.ivClient, 0);
            byte[] hmac = CryptoTools.getHMAC(dataToSign, keySet.hmacKey);

            data = new byte[OnionObject.MAX_HOPS * OnionObject.TOTAL_LENGTH];

            System.arraycopy(keyServer.getPubKey(), 0, data, 0, OnionObject.KEY_LENGTH);
            System.arraycopy(hmac, 0, data, OnionObject.KEY_LENGTH, hmac.length);
            System.arraycopy(encryptedTemp, 0, data,
                    OnionObject.KEY_LENGTH + OnionObject.HMAC_LENGTH, encryptedTemp.length - OnionObject.KEY_LENGTH - OnionObject.HMAC_LENGTH);

        }

        return new OnionObject(data);
    }
}
