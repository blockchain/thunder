package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.messages.*;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;

import java.util.List;

//Slowly moving away from not storing state in PaymentLogic anymore, but just business logic
public interface LNPaymentLogic {
    Transaction getChannelTransaction (TransactionOutPoint anchor, ChannelStatus channelStatus, ECKey client, ECKey server);
    ChannelSignatures getSignatureObject (Channel channel, Transaction channelTransaction, List<Transaction> paymentTransactions);
    List<Transaction> getPaymentTransactions (Sha256Hash parentTransactionHash, ChannelStatus channelStatus, ECKey keyServer, ECKey keyClient);

    void checkUpdate (LNConfiguration configuration, Channel channel, ChannelUpdate channelUpdate);
    void checkSignatures (ECKey keyServer, ECKey keyClient, ChannelSignatures channelSignatures, Transaction channelTransaction, List<Transaction>
            paymentTransactions, ChannelStatus status);

    Transaction getChannelTransaction (Channel channel, SIDE side);
    ChannelSignatures getSignatureObject (Channel channel);
    List<Transaction> getPaymentTransactions (Channel channel, SIDE side);
    void checkSignatures (Channel channel, ChannelSignatures channelSignatures, SIDE side);

    public enum SIDE {
        SERVER,
        CLIENT
    }
}
