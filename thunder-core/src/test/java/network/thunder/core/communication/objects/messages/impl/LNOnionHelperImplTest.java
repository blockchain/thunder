package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by matsjerratsch on 25/01/2016.
 */
public class LNOnionHelperImplTest {

    List<ECKey> keyList = new ArrayList<>();

    LNOnionHelper onionHelper = new LNOnionHelperImpl();

    @Before
    public void prepare () {

    }

    @Test
    public void shouldBuildAndDeconstructCorrectFullOnion () {
        buildKeylist(OnionObject.MAX_HOPS);

        OnionObject object = onionHelper.createOnionObject(getByteList(keyList));

        List<byte[]> listFromOnion = new ArrayList<>();
        listFromOnion.add(keyList.get(0).getPubKey());

        for (ECKey key : keyList) {
            LNOnionHelper helperTemp = new LNOnionHelperImpl();
            helperTemp.init(key);
            helperTemp.loadMessage(null, object);

            listFromOnion.add(helperTemp.getNextHop().getPubKey());
            object = helperTemp.getMessageForNextHop();

        }

        for (int i = 0; i < keyList.size(); ++i) {
            ECKey key = keyList.get(i);
            byte[] keyOnion = listFromOnion.get(i);
            System.out.println(Tools.bytesToHex(key.getPubKey()) + " " + Tools.bytesToHex(keyOnion));
            assertTrue(Arrays.equals(key.getPubKey(), keyOnion));
        }
    }

    @Test
    public void shouldBuildAndDeconstructCorrectHalfOnion () {
        buildKeylist(OnionObject.MAX_HOPS - 4);

        OnionObject object = onionHelper.createOnionObject(getByteList(keyList));

        List<byte[]> listFromOnion = new ArrayList<>();
        listFromOnion.add(keyList.get(0).getPubKey());

        for (ECKey key : keyList) {
            LNOnionHelper helperTemp = new LNOnionHelperImpl();
            helperTemp.init(key);
            helperTemp.loadMessage(null, object);

            listFromOnion.add(helperTemp.getNextHop().getPubKey());
            object = helperTemp.getMessageForNextHop();

        }

        for (int i = 0; i < keyList.size(); ++i) {
            ECKey key = keyList.get(i);
            byte[] keyOnion = listFromOnion.get(i);
            System.out.println(Tools.bytesToHex(key.getPubKey()) + " " + Tools.bytesToHex(keyOnion));
            assertTrue(Arrays.equals(key.getPubKey(), keyOnion));
        }
    }

    public void buildKeylist (int hops) {
        for (int i = 0; i < hops; ++i) {
            keyList.add(new ECKey());
            System.out.println(Tools.bytesToHex(keyList.get(i).getPubKey()));
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