package network.thunder.core.helper.blockchain;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

public interface BlockchainHelper {
    boolean broadcastTransaction (Transaction tx);

    void addTxListener (OnTxCommand executor);

    void addBlockListener (OnBlockCommand executor);
    Transaction getTransaction (Sha256Hash hash);
    int getHeight ();

}
