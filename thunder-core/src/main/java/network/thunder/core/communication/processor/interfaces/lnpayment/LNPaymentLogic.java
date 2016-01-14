package network.thunder.core.communication.processor.interfaces.lnpayment;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.lightning.RevocationHash;
import org.bitcoinj.core.Transaction;

/**
 * Created by matsjerratsch on 08/01/2016.
 */
public interface LNPaymentLogic {

    public Transaction getClientTransaction ();

    public Transaction getServerTransaction ();

    public void checkMessage (LNPayment message);

    public ChannelStatus getTemporaryChannelStatus ();

    public void putCurrentRevocationHashServer (RevocationHash revocationHash);

    public void putNewChannelStatus (ChannelStatus channelStatus);

}
