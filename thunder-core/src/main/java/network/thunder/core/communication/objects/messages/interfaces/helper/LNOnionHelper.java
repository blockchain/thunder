package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import org.bitcoinj.core.ECKey;

import java.util.List;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public interface LNOnionHelper {
    void init (ECKey keyServer);

    void loadMessage (OnionObject encryptedOnionObject);

    ECKey getNextHop ();

    OnionObject getMessageForNextHop ();

    OnionObject createOnionObject (List<byte[]> keyList);

    boolean isLastHop ();

}
