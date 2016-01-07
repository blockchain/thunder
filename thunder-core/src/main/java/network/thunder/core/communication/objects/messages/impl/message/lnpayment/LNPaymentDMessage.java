package network.thunder.core.communication.objects.messages.impl.message.lnpayment;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.lightning.RevocationHash;

import java.util.ArrayList;

/**
 * Created by matsjerratsch on 07/01/2016.
 */
public class LNPaymentDMessage implements LNPayment {

    ArrayList<RevocationHash> oldRevocationHashes;

    @Override
    public void verify () {
        Preconditions.checkNotNull(oldRevocationHashes);
    }
}
