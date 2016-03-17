package network.thunder.core.communication.processor.interfaces.lnpayment;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.database.objects.Channel;

/**
 * Created by matsjerratsch on 08/01/2016.
 */
public interface LNPaymentLogic {

    void initialise (Channel channel);

    void checkMessageIncoming (LNPayment message);

    Channel updateChannel (Channel channel);

    ChannelStatus getTemporaryChannelStatus ();

    LNPaymentAMessage getAMessage (ChannelStatus newStatus);

    LNPaymentBMessage getBMessage ();

    LNPaymentCMessage getCMessage ();

    LNPaymentDMessage getDMessage ();
}
