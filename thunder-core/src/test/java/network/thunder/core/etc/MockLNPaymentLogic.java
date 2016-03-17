package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

/**
 * Created by matsjerratsch on 13/01/2016.
 */
public class MockLNPaymentLogic implements LNPaymentLogic {
    ChannelStatus status;
    Channel channel;

    LNPaymentMessageFactory messageFactory;

    public MockLNPaymentLogic (LNPaymentMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
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
            status = ((LNPaymentAMessage) message).channelStatus.getCloneReversed();
        }
    }

    public void readMessageOutbound (LNPayment message) {
        if (message instanceof LNPaymentAMessage) {
            status = ((LNPaymentAMessage) message).channelStatus;
        }
    }

    @Override
    public Channel updateChannel (Channel channel) {
        return channel;
    }

    @Override
    public ChannelStatus getTemporaryChannelStatus () {
        return status;
    }

    @Override
    public LNPaymentAMessage getAMessage (ChannelStatus newStatus) {
        this.status = newStatus;
        return messageFactory.getMessageA(channel, status);
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
