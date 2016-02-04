package network.thunder.core.communication.processor.interfaces.lnpayment;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.core.Transaction;

/**
 * Created by matsjerratsch on 08/01/2016.
 */
public interface LNPaymentLogic {

    void initialise (Channel channel);

    Transaction getClientTransaction ();

    Transaction getServerTransaction ();

    void checkMessageIncoming (LNPayment message);

    void readMessageOutbound (LNPayment message);

    ChannelStatus getTemporaryChannelStatus ();
}
