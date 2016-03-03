package network.thunder.core.communication.objects.messages.impl.message.lnpayment;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.lightning.RevocationHash;

import java.util.List;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNPaymentBMessage implements LNPayment {

    public RevocationHash newRevocation;

    public boolean success = true;
    public String error;
    //TODO implement returning problematic payments, such that live locks can be resolved properly.
    public List<PaymentData> problematicPayments;

    public LNPaymentBMessage (RevocationHash newRevocation) {
        this.newRevocation = newRevocation;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(newRevocation);
    }

    public LNPaymentBMessage (boolean success, String error) {
        this.success = success;
        this.error = error;
    }
}
