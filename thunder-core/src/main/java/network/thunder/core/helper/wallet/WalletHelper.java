package network.thunder.core.helper.wallet;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;

public interface WalletHelper {
    long getSpendableAmount ();

    Transaction completeInputs (Transaction transaction);

    Address fetchAddress ();
}
