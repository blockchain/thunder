package network.thunder.core.communication.objects.messages.interfaces.helper.etc;

import org.bitcoinj.core.Transaction;

public interface OnTxCommand {
    boolean compare (Transaction tx);

    void execute (Transaction tx);
}
