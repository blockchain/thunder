package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.core.Transaction;

/**
 * Created by matsjerratsch on 13/01/2016.
 */
public class MockLNPaymentLogic implements LNPaymentLogic {
    ChannelStatus status;

    @Override
    public void initialise (Channel channel) {

    }

    @Override
    public Transaction getClientTransaction () {
        return null;
    }

    @Override
    public Transaction getServerTransaction () {
        return null;
    }

    @Override
    public void checkMessageIncoming (LNPayment message) {
        if (message instanceof LNPaymentAMessage) {
            status = ((LNPaymentAMessage) message).channelStatus;
        }
    }

    @Override
    public void readMessageOutbound (LNPayment message) {
        if (message instanceof LNPaymentAMessage) {
            status = ((LNPaymentAMessage) message).channelStatus;
        }
    }

    @Override
    public ChannelStatus getTemporaryChannelStatus () {
        return status;
    }

}
