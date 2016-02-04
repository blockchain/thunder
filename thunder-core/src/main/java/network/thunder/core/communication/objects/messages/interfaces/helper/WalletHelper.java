package network.thunder.core.communication.objects.messages.interfaces.helper;

import org.bitcoinj.core.Transaction;

/**
 * Created by matsjerratsch on 19/01/2016.
 */
public interface WalletHelper {
    long getSpendableAmount ();

    Transaction completeInputs (Transaction transaction);
}
