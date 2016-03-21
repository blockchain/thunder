package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentAMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentBMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentCMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentDMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPayment;
import network.thunder.core.communication.layer.high.Channel;

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
