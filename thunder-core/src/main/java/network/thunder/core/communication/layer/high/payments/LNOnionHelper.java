package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import org.bitcoinj.core.ECKey;

import java.util.List;

public interface LNOnionHelper {
    int BYTES_OFFSET_PER_ENCRYPTION = 150;

    PeeledOnion loadMessage (ECKey keyServer, OnionObject encryptedOnionObject);

    OnionObject createOnionObject (List<byte[]> keyList, byte[] payload);

}
