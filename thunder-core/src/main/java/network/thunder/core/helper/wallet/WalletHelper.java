package network.thunder.core.helper.wallet;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;

public interface WalletHelper {
    long getSpendableAmount ();

    Transaction addInputs (Transaction transaction, long value, float feePerByte);

    Transaction signTransaction(Transaction transaction);

    Address fetchAddress ();
}
