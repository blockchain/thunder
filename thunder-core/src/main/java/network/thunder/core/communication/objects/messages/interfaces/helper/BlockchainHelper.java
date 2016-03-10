package network.thunder.core.communication.objects.messages.interfaces.helper;

import network.thunder.core.communication.objects.messages.interfaces.helper.etc.OnBlockCommand;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.OnTxCommand;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

public interface BlockchainHelper {
    public boolean broadcastTransaction (Transaction tx);

    public void addTxListener (OnTxCommand executor);

    public void addBlockListener (OnBlockCommand executor);

    public Transaction getTransaction (Sha256Hash hash);

    public void init ();
}
