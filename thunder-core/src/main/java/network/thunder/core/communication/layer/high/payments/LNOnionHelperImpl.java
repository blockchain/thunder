package network.thunder.core.communication.layer.high.payments;

import com.google.common.collect.Lists;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import network.thunder.core.communication.layer.high.payments.messages.DecryptedReceiverObject;
import network.thunder.core.communication.layer.high.payments.messages.EncryptedReceiverObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.Fee;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.crypto.CryptoTools;
import network.thunder.core.helper.crypto.ECDH;
import network.thunder.core.helper.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LNOnionHelperImpl implements LNOnionHelper {

    @Override
    public PeeledOnion loadMessage (ECKey key, OnionObject encryptedOnionObject) {
        ECDHKeySet keySet = getKeySet(key, encryptedOnionObject);

        byte[] unencrypted = decryptMessage(keySet, encryptedOnionObject);
        byte[] payload = new byte[OnionObject.DATA_LENGTH];
        System.arraycopy(unencrypted, 0, payload, 0, OnionObject.DATA_LENGTH);

        OnionObject nextObject = getMessageForNextHop(keySet, unencrypted);
        nextObject.dataFinalReceiver = encryptedOnionObject.dataFinalReceiver;

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

        if (payload != null && payload.length % OnionObject.TOTAL_LENGTH != 0) {
            throw new RuntimeException("Payload not a multiple of OnionObject.TOTAL_LENGTH");
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

    private static OnionObject addOnionLayer (OnionObject core, PeeledOnion layerToAdd, ECKey keyNode) {
        int byteCount = core.data.length;
        byte[] temp = new byte[byteCount];
        byte[] inner = core.data;
        byte[] dataToSign = new byte[OnionObject.DATA_LENGTH];

        //Shifting the inner core by DATA_LENGTH
        System.arraycopy(inner, 0, temp, OnionObject.DATA_LENGTH, inner.length - OnionObject.DATA_LENGTH);

        byte[] data = layerToAdd.getData();
        System.arraycopy(data, 0, dataToSign, 0, data.length);
        System.arraycopy(data, 0, temp, 0, data.length);

        ECKey keyServer = CryptoTools.getEphemeralKey();
        ECDHKeySet keySet = ECDH.getSharedSecret(keyServer, keyNode);

        byte[] hmac = CryptoTools.getHMAC(dataToSign, keySet.hmacKey);
        byte[] encryptedTemp = CryptoTools.encryptAES_CTR(temp, keySet.encryptionKey, keySet.ivClient, 0);

        data = new byte[byteCount];

        System.arraycopy(keyServer.getPubKey(), 0, data, 0, OnionObject.KEY_LENGTH);
        System.arraycopy(hmac, 0, data, OnionObject.KEY_LENGTH, hmac.length);
        System.arraycopy(encryptedTemp, 0, data,
                OnionObject.KEY_LENGTH + OnionObject.HMAC_LENGTH, encryptedTemp.length - OnionObject.KEY_LENGTH - OnionObject.HMAC_LENGTH);

        OnionObject onionObject = new OnionObject(data);
        onionObject.dataFinalReceiver = core.dataFinalReceiver;
        return onionObject;
    }

    @Override
    public OnionObject createOnionObject (List<ChannelStatusObject> route,
                                          NodeKey finalReceiver,
                                          int timeout,
                                          long amount,
                                          @Nullable PaymentSecret paymentSecret) {
        List<ChannelStatusObject> routeInvers = Lists.reverse(new ArrayList<>(route));
        List<Fee> feeList = Tools.getFeeList(route, finalReceiver);
        PeeledOnion peeledOnion = new PeeledOnion();
        int byteCount = OnionObject.MAX_HOPS * OnionObject.TOTAL_LENGTH;
        byte[] data = Tools.getRandomByte(byteCount);
        OnionObject onionObject = new OnionObject(data);

        peeledOnion.isLastHop = true;
        if (paymentSecret != null) {
            peeledOnion.containsSecret = true;
            peeledOnion.paymentSecret = paymentSecret;
        } else {
            peeledOnion.containsSecret = false;
        }
        peeledOnion.timeoutRemoved = 0;
        peeledOnion.timeout = timeout - route.stream().mapToInt(o -> o.minTimeout * 2).sum();
        peeledOnion.fee = 0;
        peeledOnion.amount = amount - Tools.calculateFee(amount, feeList);

        ECKey key = finalReceiver.getECKey();
        onionObject = addOnionLayer(onionObject, peeledOnion, key);

        NodeKey nextNode = new NodeKey(key);

        for (ChannelStatusObject c : routeInvers) {

            key = ECKey.fromPublicOnly(c.getOtherNode(key.getPubKey()));

            peeledOnion = new PeeledOnion();
            peeledOnion.isLastHop = false;
            peeledOnion.nextHop = nextNode;
            peeledOnion.containsSecret = false;
            peeledOnion.onionObject = onionObject;
            peeledOnion.amount = amount - Tools.calculateFee(amount, feeList);
            peeledOnion.fee = feeList.get(feeList.size() - 1).calculateFee(peeledOnion.amount);
            peeledOnion.timeoutRemoved = (short) (c.minTimeout * 2);
            peeledOnion.timeout = timeout - route.subList(0, route.size() - 1).stream().mapToInt(o -> o.minTimeout * 2).sum();

            onionObject = addOnionLayer(onionObject, peeledOnion, key);

            nextNode = new NodeKey(key);

            route.remove(route.size() - 1);
            feeList.remove(feeList.size() - 1);
        }

        return onionObject;
    }

    @Override
    public OnionObject createOnionObject (List<ChannelStatusObject> route,
                                          NodeKey rpNode,
                                          OnionObject rpObject,
                                          long amount,
                                          @Nullable PaymentSecret paymentSecret,
                                          @Nullable ECKey ephemeralReceiver) {
        route = new ArrayList<>(route);
        List<ChannelStatusObject> routeInvers = Lists.reverse(new ArrayList<>(route));
        List<Fee> feeList = Tools.getFeeList(route, rpNode);

        byte[] fullOnion = new byte[OnionObject.TOTAL_LENGTH * OnionObject.MAX_HOPS];
        System.arraycopy(rpObject.data, 0, fullOnion, 0, rpObject.data.length);
        OnionObject onionObject = new OnionObject(fullOnion);

        if (paymentSecret != null && ephemeralReceiver != null) {
            DecryptedReceiverObject receiverObject = new DecryptedReceiverObject();
            receiverObject.secret = paymentSecret;
            receiverObject.amount = amount - Tools.calculateFee(amount, feeList);

            ECKey keyServer = CryptoTools.getEphemeralKey();
            ECDHKeySet keySet = ECDH.getSharedSecret(keyServer, ephemeralReceiver);
            EncryptedReceiverObject encryptedReceiverObject = new EncryptedReceiverObject();
            encryptedReceiverObject.ephemeralPubKeyHashReceiver = Sha256Hash.hash(ephemeralReceiver.getPubKey());
            encryptedReceiverObject.ephemeralPubKeySender = keyServer.getPubKey();
            encryptedReceiverObject.data = CryptoTools.encryptAES_CTR(
                    receiverObject.getData(),
                    keySet.encryptionKey,
                    keySet.ivClient,
                    0);

            encryptedReceiverObject.hmac = CryptoTools.getHMAC(receiverObject.getData(), keySet.hmacKey);
            onionObject.dataFinalReceiver = encryptedReceiverObject;
        }

        PeeledOnion peeledOnion = new PeeledOnion();
        ECKey key = rpNode.getECKey();
        NodeKey nextNode = new NodeKey(key);
        peeledOnion.nextHop = rpNode;

        for (ChannelStatusObject c : routeInvers) {
            key = ECKey.fromPublicOnly(c.getOtherNode(key.getPubKey()));
            peeledOnion = new PeeledOnion();
            peeledOnion.isLastHop = false;
            peeledOnion.containsSecret = false;
            peeledOnion.onionObject = onionObject;
            peeledOnion.amount = 0;
            peeledOnion.fee = 0;
            peeledOnion.timeoutRemoved = 0;
            peeledOnion.timeout = 0;
            peeledOnion.nextHop = nextNode;

            onionObject = addOnionLayer(onionObject, peeledOnion, key);
            nextNode = new NodeKey(key);

            route.remove(route.size() - 1);
            feeList.remove(feeList.size() - 1);
        }
        return onionObject;
    }

    @Override
    public OnionObject createRPObject (List<ChannelStatusObject> route, NodeKey keyServer) {
        route = new ArrayList<>(route);
        PeeledOnion peeledOnion = new PeeledOnion();
        peeledOnion.isLastHop = true;
        byte[] randomData = Tools.getRandomByte((route.size() + 1) * OnionObject.TOTAL_LENGTH);
        OnionObject onionObject = new OnionObject(randomData);
        onionObject = addOnionLayer(onionObject, peeledOnion, keyServer.getECKey());

        List<ChannelStatusObject> routeInvers = Lists.reverse(new ArrayList<>(route));
        ECKey key = keyServer.getECKey();
        NodeKey nextNode = new NodeKey(key);

        for (ChannelStatusObject c : routeInvers) {
            key = ECKey.fromPublicOnly(c.getOtherNode(key.getPubKey()));

            peeledOnion = new PeeledOnion();
            peeledOnion.isLastHop = false;
            peeledOnion.containsSecret = false;
            peeledOnion.onionObject = onionObject;
            peeledOnion.amount = 0;
            peeledOnion.fee = 0;
            peeledOnion.timeoutRemoved = 0;
            peeledOnion.timeout = 0;
            peeledOnion.nextHop = nextNode;

            onionObject = addOnionLayer(onionObject, peeledOnion, key);
            nextNode = new NodeKey(key);
            route.remove(route.size() - 1);
        }
        return onionObject;
    }
}
