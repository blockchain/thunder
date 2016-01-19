package network.thunder.core.communication.processor.interfaces.lnpayment;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.core.Transaction;

/**
 * Created by matsjerratsch on 08/01/2016.
 */
public interface LNPaymentLogic {

    public void initialise (Channel channel);

    public Transaction getClientTransaction ();

    public Transaction getServerTransaction ();

    public void checkMessageIncoming (LNPayment message);

    public void readMessageOutbound (LNPayment message);

    public ChannelStatus getTemporaryChannelStatus ();
}
