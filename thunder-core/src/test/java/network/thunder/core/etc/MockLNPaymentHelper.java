package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;

/**
 * Created by matsjerratsch on 03/02/2016.
 */
public class MockLNPaymentHelper implements LNPaymentHelper {
    @Override
    public void addProcessor (LNPaymentProcessor processor) {

    }

    @Override
    public void removeProcessor (LNPaymentProcessor processor) {

    }

    @Override
    public void relayPayment (LNPaymentProcessor paymentProcessor, PaymentData paymentData) {

    }

    @Override
    public void makePayment (PaymentData paymentData) {
        
    }

    @Override
    public void paymentRedeemed (PaymentSecret paymentSecret) {

    }

    @Override
    public void paymentRefunded (PaymentData paymentSecret) {

    }
}
