package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.messages.*;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;

//Slowly moving away from not storing state in PaymentLogic anymore, but just business logic
public interface LNPaymentLogic {
    ChannelSignatures getSignatureObject (Channel channel, Transaction channelTransaction);
    void initialise (Channel channel);

    void checkMessageIncoming (LNPayment message);

    Transaction getChannelTransaction (TransactionOutPoint anchor, ChannelStatus channelStatus, ECKey client, ECKey server);
    void checkSignatures (ECKey keyServer, ECKey keyClient, ChannelSignatures channelSignatures, Transaction channelTransaction, ChannelStatus status);

    Channel updateChannel (Channel channel);

    ChannelUpdate getChannelUpdate ();

    LNPaymentAMessage getAMessage (ChannelUpdate update);

    LNPaymentBMessage getBMessage ();

    LNPaymentCMessage getCMessage ();

    LNPaymentDMessage getDMessage ();
}
