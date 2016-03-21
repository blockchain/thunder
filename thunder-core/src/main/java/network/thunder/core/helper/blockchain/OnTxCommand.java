package network.thunder.core.helper.blockchain;

import org.bitcoinj.core.Transaction;

public interface OnTxCommand {
    boolean compare (Transaction tx);

    void execute (Transaction tx);
}
