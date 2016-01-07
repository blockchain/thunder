package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentOnionMessageEncrypted;
import org.bitcoinj.core.ECKey;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public interface LNOnionHelper {
    void init (ECKey keyServer);

    void loadMessage (ECKey receivedHop, LNPaymentOnionMessageEncrypted encryptedOnionObject);

    ECKey getNextHop ();

    LNPaymentOnionMessageEncrypted getMessageForNextHop ();

}
