package network.thunder.core.communication.processor.interfaces.lnpayment;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.database.objects.Channel;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

/**
 * Created by matsjerratsch on 08/01/2016.
 */
public interface LNPaymentLogic {

    void initialise (Channel channel);

    Transaction getClientTransaction ();

    Transaction getServerTransaction ();

    List<Transaction> getClientPaymentTransactions ();

    List<Transaction> getServerPaymentTransactions ();

    List<TransactionSignature> getChannelSignatures ();

    List<TransactionSignature> getPaymentSignatures ();

    void checkMessageIncoming (LNPayment message);

    void readMessageOutbound (LNPayment message);

    Channel updateChannel(Channel channel);

    ChannelStatus getTemporaryChannelStatus ();
}
