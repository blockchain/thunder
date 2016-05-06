package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.crypto.CryptoTools;
import network.thunder.core.helper.crypto.ECDH;
import network.thunder.core.helper.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;

import java.util.List;

public class LNOnionHelperImpl implements LNOnionHelper {

    @Override
    public PeeledOnion loadMessage (ECKey key, OnionObject encryptedOnionObject) {
        ECDHKeySet keySet = getKeySet(key, encryptedOnionObject);

        byte[] unencrypted = decryptMessage(keySet, encryptedOnionObject);
        byte[] payload = new byte[OnionObject.DATA_LENGTH];
        System.arraycopy(unencrypted, 0, payload, 0, OnionObject.DATA_LENGTH);

        OnionObject nextObject = getMessageForNextHop(keySet, unencrypted);

        return new PeeledOnion(nextObject, payload);
    }

    private static OnionObject getMessageForNextHop (ECDHKeySet keySet, byte[] decryptedData) {
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

    private static byte[] decryptMessage (ECDHKeySet keySet, OnionObject encryptedOnionObject) {
        byte[] hmac = new byte[OnionObject.HMAC_LENGTH];
        byte[] data = new byte[OnionObject.TOTAL_LENGTH * OnionObject.MAX_HOPS - (OnionObject.KEY_LENGTH + OnionObject.HMAC_LENGTH)];

        System.arraycopy(encryptedOnionObject.data, OnionObject.KEY_LENGTH, hmac, 0, hmac.length);
        System.arraycopy(encryptedOnionObject.data, OnionObject.KEY_LENGTH + hmac.length, data, 0, data.length);

        byte[] decryptedData = CryptoTools.decryptAES_CTR(data, keySet.encryptionKey, keySet.ivServer, 0);

        byte[] dataToSign = new byte[OnionObject.DATA_LENGTH];
        System.arraycopy(decryptedData, 0, dataToSign, 0, OnionObject.DATA_LENGTH);

        CryptoTools.checkHMAC(hmac, dataToSign, keySet.hmacKey);

        return decryptedData;
    }

    private static ECDHKeySet getKeySet (ECKey keyServer, OnionObject encryptedOnionObject) {
        byte[] key = new byte[OnionObject.KEY_LENGTH];

        System.arraycopy(encryptedOnionObject.data, 0, key, 0, key.length);

        ECKey ephemeralKey = ECKey.fromPublicOnly(key);
        ECDHKeySet keySet = ECDH.getSharedSecret(keyServer, ephemeralKey);
        return keySet;
    }

    @Override
    public OnionObject createOnionObject (List<byte[]> nodeList, byte[] payload) {
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
