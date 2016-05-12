package network.thunder.core.etc;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.high.payments.messages.*;
import org.bitcoinj.core.ECKey;
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

    @Override
    public ChannelSignatures getSignatureObject (Channel channel, Transaction channelTransaction) {
        return null;
    }

    @Override
    public void initialise (Channel channel) {
        this.channel = channel;
    }

    public List<TransactionSignature> getChannelSignatures () {
        return null;
    }

    public List<TransactionSignature> getPaymentSignatures () {
        return null;
    }

    @Override
    public void checkMessageIncoming (LNPayment message) {
        if (message instanceof LNPaymentAMessage) {
            update = ((LNPaymentAMessage) message).channelStatus.getCloneReversed();
        }
    }

    @Override
    public Transaction getChannelTransaction (TransactionOutPoint anchor, ChannelStatus channelStatus, ECKey client, ECKey server) {
        return null;
    }


    @Override
    public void checkSignatures (ECKey keyServer, ECKey keyClient, ChannelSignatures channelSignatures, Transaction channelTransaction, ChannelStatus status) {

    }

    public void readMessageOutbound (LNPayment message) {
        if (message instanceof LNPaymentAMessage) {
            update = ((LNPaymentAMessage) message).channelStatus;
        }
    }

    @Override
    public Channel updateChannel (Channel channel) {
        return channel;
    }

    @Override
    public ChannelUpdate getChannelUpdate () {
        return update;
    }

    @Override
    public LNPaymentAMessage getAMessage (ChannelUpdate update) {
        this.update = update;
        return messageFactory.getMessageA(channel, update);
    }

    @Override
    public LNPaymentBMessage getBMessage () {
        return messageFactory.getMessageB(channel);
    }

    @Override
    public LNPaymentCMessage getCMessage () {
        return messageFactory.getMessageC(channel, null, null);
    }

    @Override
    public LNPaymentDMessage getDMessage () {
        return messageFactory.getMessageD(channel);
    }

}
