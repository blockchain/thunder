package network.thunder.core.communication.objects.messages.impl.message.lnpayment;

import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public class LNPaymentOnionMessageEncrypted implements LNPayment {
    public byte[] ephemeralKey;
    public byte[] hmac;
    public byte[] data;

    @Override
    public void verify () {

    }
}
