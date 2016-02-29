package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.PeeledOnion;
import org.bitcoinj.core.ECKey;

import java.util.List;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public interface LNOnionHelper {
    int BYTES_OFFSET_PER_ENCRYPTION = 150;

    PeeledOnion loadMessage (ECKey keyServer, OnionObject encryptedOnionObject);

    OnionObject createOnionObject (List<byte[]> keyList, byte[] payload);

}
