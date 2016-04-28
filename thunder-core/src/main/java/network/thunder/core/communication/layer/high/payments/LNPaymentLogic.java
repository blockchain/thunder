package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.messages.*;

public interface LNPaymentLogic {

    void initialise (Channel channel);

    void checkMessageIncoming (LNPayment message);

    Channel updateChannel (Channel channel);

    ChannelUpdate getChannelUpdate ();

    LNPaymentAMessage getAMessage (ChannelUpdate update);

    LNPaymentBMessage getBMessage ();

    LNPaymentCMessage getCMessage ();

    LNPaymentDMessage getDMessage ();
}
