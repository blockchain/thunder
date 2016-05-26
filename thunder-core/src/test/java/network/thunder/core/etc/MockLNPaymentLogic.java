package network.thunder.core.etc;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.high.payments.messages.*;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

public class MockLNPaymentLogic implements LNPaymentLogic {
    ChannelStatus status;
    ChannelUpdate update;
    Channel channel;

    LNPaymentMessageFactory messageFactory;

    public MockLNPaymentLogic (LNPaymentMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }



    public List<TransactionSignature> getChannelSignatures () {
        return null;
    }

    public List<TransactionSignature> getPaymentSignatures () {
        return null;
    }


    @Override
    public Transaction getChannelTransaction (TransactionOutPoint anchor, ChannelStatus channelStatus, ECKey client, ECKey server) {
        return null;
    }

    @Override
    public ChannelSignatures getSignatureObject (Channel channel, Transaction channelTransaction, List<Transaction> paymentTransactions) {
        return null;
    }

    @Override
    public List<Transaction> getPaymentTransactions (Sha256Hash parentTransactionHash, ChannelStatus channelStatus, ECKey keyServer, ECKey keyClient) {
        return null;
    }

    @Override
    public void checkUpdate (LNConfiguration configuration, Channel channel, ChannelUpdate channelUpdate) {

    }

    @Override
    public void checkSignatures (ECKey keyServer, ECKey keyClient, ChannelSignatures channelSignatures, Transaction channelTransaction, List<Transaction>
            paymentTransactions, ChannelStatus status) {

    }

    @Override
    public Transaction getChannelTransaction (Channel channel, SIDE side) {
        return null;
    }

    @Override
    public ChannelSignatures getSignatureObject (Channel channel) {
        return null;
    }

    @Override
    public List<Transaction> getPaymentTransactions (Channel channel, SIDE side) {
        return null;
    }

    @Override
    public void checkSignatures (Channel channel, ChannelSignatures channelSignatures, SIDE side) {

    }

    public void readMessageOutbound (LNPayment message) {
        if (message instanceof LNPaymentAMessage) {
            update = ((LNPaymentAMessage) message).channelUpdate;
        }
    }




}
