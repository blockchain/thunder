package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;

import java.util.Random;

/**
 * Created by matsjerratsch on 08/01/2016.
 */
public class LNPaymentMessageFactoryImpl extends MesssageFactoryImpl implements LNPaymentMessageFactory {

    @Override
    public LNPaymentAMessage getMessageA () {
        LNPaymentAMessage message = new LNPaymentAMessage();
        message.dice = new Random().nextInt(Integer.MAX_VALUE);
        return message;
    }

    @Override
    public LNPaymentBMessage getMessageB () {
        return new LNPaymentBMessage();
    }

    @Override
    public LNPaymentCMessage getMessageC () {
        return new LNPaymentCMessage();
    }

    @Override
    public LNPaymentDMessage getMessageD () {
        return new LNPaymentDMessage();
    }
}
