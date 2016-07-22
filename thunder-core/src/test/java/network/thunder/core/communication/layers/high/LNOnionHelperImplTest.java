package network.thunder.core.communication.layers.high;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.payments.LNOnionHelper;
import network.thunder.core.communication.layer.high.payments.LNOnionHelperImpl;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.DecryptedReceiverObject;
import network.thunder.core.communication.layer.high.payments.messages.EncryptedReceiverObject;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.etc.TestTools;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.crypto.CryptoTools;
import network.thunder.core.helper.crypto.ECDH;
import network.thunder.core.helper.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class LNOnionHelperImplTest {

    List<ECKey> keyList = new ArrayList<>();

    LNOnionHelper onionHelper = new LNOnionHelperImpl();

    @Before
    public void prepare () {

    }

    @Test
    public void shouldBuildAndDeconstructCorrectFullPlainOnion () {
        buildKeylist(OnionObject.MAX_HOPS);

        OnionObject object = onionHelper.createOnionObject(getByteList(keyList), null);

        List<byte[]> listFromOnion = new ArrayList<>();
        listFromOnion.add(keyList.get(0).getPubKey());

        for (ECKey key : keyList) {
            LNOnionHelper helperTemp = new LNOnionHelperImpl();
            PeeledOnion peeledOnion = helperTemp.loadMessage(key, object);

            if (peeledOnion.isLastHop) {
                listFromOnion.add(key.getPubKey());
            } else {
                listFromOnion.add(peeledOnion.nextHop.getPubKey());
                object = peeledOnion.onionObject;
            }
        }

        for (int i = 0; i < keyList.size(); ++i) {
            ECKey key = keyList.get(i);
            byte[] keyOnion = listFromOnion.get(i);
            assertTrue(Arrays.equals(key.getPubKey(), keyOnion));
        }
    }

    @Test
    public void shouldBuildAndDeconstructCorrectFullMetaDataOnion () {
        buildKeylist(OnionObject.MAX_HOPS);
        List<ChannelStatusObject> route = TestTools.translateECKeyToRoute(this.keyList);
        NodeKey finalReceiver = NodeKey.wrap(this.keyList.get(this.keyList.size() - 1));

        OnionObject object = onionHelper.createOnionObject(route, finalReceiver, 1000, 100000, null);

        List<byte[]> listFromOnion = new ArrayList<>();
        listFromOnion.add(keyList.get(0).getPubKey());

        for (ECKey key : keyList) {
            LNOnionHelper helperTemp = new LNOnionHelperImpl();
            PeeledOnion peeledOnion = helperTemp.loadMessage(key, object);

            if (peeledOnion.isLastHop) {
                listFromOnion.add(key.getPubKey());
            } else {
                listFromOnion.add(peeledOnion.nextHop.getPubKey());
                object = peeledOnion.onionObject;
            }
        }

        for (int i = 0; i < keyList.size(); ++i) {
            ECKey key = keyList.get(i);
            byte[] keyOnion = listFromOnion.get(i);
            assertTrue(Arrays.equals(key.getPubKey(), keyOnion));
        }
    }

    @Test
    public void shouldBuildAndDeconstructCorrectFullRPOnion () {
        final int RP_SIZE = 1;
        buildKeylist(OnionObject.MAX_HOPS);
        List<ChannelStatusObject> route = TestTools.translateECKeyToRoute(this.keyList);
        NodeKey finalReceiver = NodeKey.wrap(this.keyList.get(this.keyList.size() - 1));

        List<ChannelStatusObject> rpRoute = route.subList(route.size() - RP_SIZE, route.size());
        List<ChannelStatusObject> payerRoute = route.subList(0, route.size() - RP_SIZE);

        OnionObject rpObject = onionHelper.createRPObject(rpRoute, finalReceiver);
        NodeKey rpNode = new NodeKey(keyList.get(keyList.size() - RP_SIZE - 1));

        ECKey ephemeralKeyReceiver = new ECKey();
        PaymentSecret paymentSecret = new PaymentSecret(Tools.getRandomByte(20));

        OnionObject object = onionHelper.createOnionObject(payerRoute, rpNode, rpObject, 1000, paymentSecret, ephemeralKeyReceiver);

        EncryptedReceiverObject encryptedReceiverObjectFromOnion = null;
        List<byte[]> listFromOnion = new ArrayList<>();
        listFromOnion.add(keyList.get(0).getPubKey());

        for (ECKey key : keyList) {
            LNOnionHelper helperTemp = new LNOnionHelperImpl();
            PeeledOnion peeledOnion = helperTemp.loadMessage(key, object);
            if (peeledOnion.isLastHop) {
                listFromOnion.add(key.getPubKey());
                encryptedReceiverObjectFromOnion = object.dataFinalReceiver;
            } else {
                listFromOnion.add(peeledOnion.nextHop.getPubKey());
                object = peeledOnion.onionObject;
            }
        }

        for (int i = 0; i < keyList.size(); ++i) {
            ECKey key = keyList.get(i);
            byte[] keyOnion = listFromOnion.get(i);
            assertTrue(Arrays.equals(key.getPubKey(), keyOnion));
        }

        assertNotNull(encryptedReceiverObjectFromOnion);

        ECDHKeySet keySet = ECDH.getSharedSecret(ephemeralKeyReceiver, ECKey.fromPublicOnly(encryptedReceiverObjectFromOnion.ephemeralPubKeySender));
        byte[] dataUnencrypted = CryptoTools.decryptAES_CTR(encryptedReceiverObjectFromOnion.data, keySet.encryptionKey, keySet.ivServer, 0);

        DecryptedReceiverObject decryptedReceiverObject = new DecryptedReceiverObject(dataUnencrypted);
        assertEquals(decryptedReceiverObject.secret, paymentSecret);
    }

    @Test
    public void shouldBuildAndDeconstructCorrectHalfOnion () {
        buildKeylist(OnionObject.MAX_HOPS - 4);

        OnionObject object = onionHelper.createOnionObject(getByteList(keyList), null);

        List<byte[]> listFromOnion = new ArrayList<>();
        listFromOnion.add(keyList.get(0).getPubKey());

        for (ECKey key : keyList) {
            LNOnionHelper helperTemp = new LNOnionHelperImpl();
            PeeledOnion peeledOnion = helperTemp.loadMessage(key, object);

            if (peeledOnion.isLastHop) {
                listFromOnion.add(key.getPubKey());
            } else {
                listFromOnion.add(peeledOnion.nextHop.getPubKey());
                object = peeledOnion.onionObject;
            }

        }

        for (int i = 0; i < keyList.size(); ++i) {
            ECKey key = keyList.get(i);
            byte[] keyOnion = listFromOnion.get(i);
            assertTrue(Arrays.equals(key.getPubKey(), keyOnion));
        }
    }

    public void buildKeylist (int hops) {
        for (int i = 0; i < hops; ++i) {
            keyList.add(new ECKey());
        }
    }

    public static List<byte[]> getByteList (List<ECKey> keyList) {
        List<byte[]> byteList = new ArrayList<>();
        for (ECKey key : keyList) {
            byteList.add(key.getPubKey());
        }

        return byteList;
    }

}