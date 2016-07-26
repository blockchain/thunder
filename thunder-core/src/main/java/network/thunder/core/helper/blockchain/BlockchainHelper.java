package network.thunder.core.helper.blockchain;

import network.thunder.core.etc.BlockWrapper;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

import java.util.List;

public interface BlockchainHelper {
    boolean broadcastTransaction (Transaction tx);

    void addTxListener (OnTxCommand executor);
    void addBlockListener (OnBlockCommand executor);

    Transaction getTransaction (Sha256Hash hash);
    BlockWrapper getBlock (Sha256Hash hash);
    List<BlockWrapper> getBlocksSince (int blockHeight);
    int getHeight ();

}
