package network.thunder.core.communication.objects.messages.interfaces.factories;

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;

/**
 * Created by matsjerratsch on 04/01/2016.
 */
public interface LNPaymentMessageFactory extends MessageFactory {
    LNPaymentAMessage getMessageA ();

    LNPaymentBMessage getMessageB ();

    LNPaymentCMessage getMessageC ();

    LNPaymentDMessage getMessageD ();
}
