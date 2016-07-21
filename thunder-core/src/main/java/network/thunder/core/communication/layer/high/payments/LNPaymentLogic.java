package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;

import java.util.List;

//Slowly moving away from not storing state in PaymentLogic anymore, but just business logic
public interface LNPaymentLogic {

    void checkUpdate (LNConfiguration configuration, Channel channel, ChannelUpdate channelUpdate);

    Transaction getChannelTransaction (TransactionOutPoint anchor, Channel channel);
    Transaction getChannelTransaction (Channel channel, SIDE side);

    ChannelSignatures getSignatureObject (Channel channel, ECKey keyToSign, Transaction channelTransaction, List<Transaction> paymentTransactions);
    ChannelSignatures getSignatureObject (Channel channel);

    List<Transaction> getPaymentTransactions (Sha256Hash parentTransactionHash, Channel channel);
    List<Transaction> getPaymentTransactions (Channel channel, SIDE side);

    void checkSignatures (Channel channel, ChannelSignatures channelSignatures, SIDE side);
    void checkSignatures (Channel channel, ECKey keyToVerify, ChannelSignatures channelSignatures, Transaction channelTransaction, List<Transaction> paymentTransactions);

    Transaction getSignedChannelTransaction (Channel channel);
    List<Transaction> getSignedPaymentTransactions (Channel channel);

    enum SIDE {
        SERVER,
        CLIENT
    }
}
