package network.thunder.core.helper.wallet;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;

/**
 * Created by matsjerratsch on 19/01/2016.
 */
public interface WalletHelper {
    long getSpendableAmount ();

    Transaction completeInputs (Transaction transaction);

    Address fetchAddress ();
}
