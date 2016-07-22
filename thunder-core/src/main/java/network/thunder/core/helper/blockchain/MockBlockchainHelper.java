package network.thunder.core.helper.blockchain;

import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletTransaction;

import java.util.*;

public class MockBlockchainHelper implements BlockchainHelper {
    public Set<Sha256Hash> broadcastedTransactionHashes = new HashSet<>();
    public Set<Transaction> broadcastedTransaction = new HashSet<>();

    List<OnBlockCommand> blockListener = Collections.synchronizedList(new ArrayList<>());
    List<OnTxCommand> txListener = Collections.synchronizedList(new ArrayList<>());

    List<Transaction> newTransactions = new ArrayList<>();

    Wallet wallet;

    int height = 0;

    public MockBlockchainHelper () {
    }

    public MockBlockchainHelper (Wallet wallet) {
        this.wallet = wallet;
    }

    public List<Transaction> getNewTransactions () {
        return newTransactions;
    }

    public Set<Transaction> getBroadcastedTransaction () {
        return broadcastedTransaction;
    }

    public void mockNewBlock (List<Transaction> transactions, boolean includeOthers) {
        broadcastedTransaction.addAll(transactions);
        List<Transaction> txToBroadcast = new ArrayList<>(transactions);
        if (includeOthers) {
            txToBroadcast.addAll(newTransactions);
            newTransactions.clear();
        }
        Block block = new Block(Constants.getNetwork(), Constants.getNetwork().getGenesisBlock().bitcoinSerialize());
        txToBroadcast.forEach(block::addTransaction);
        height++;
        txListener.forEach(listener -> txToBroadcast.forEach(listener::execute));
        blockListener.forEach(listener -> listener.execute(block));
    }

    @Override
    public boolean broadcastTransaction (Transaction tx) {
        if (broadcastedTransactionHashes.add(tx.getHash())) {
            System.out.println(" MockBlockchainHelper.broadcastTransaction " + height);
            System.out.println("tx = " + tx.toString());
        }
        newTransactions.add(tx);
        broadcastedTransaction.add(tx);

        //TODO not perfect yet - later we can connect multiple MockBlockchainHelper that will propagate tx across each other
        for (OnTxCommand onTxCommand : txListener) {
            if (onTxCommand.compare(tx)) {
                onTxCommand.execute(tx);
            }
        }

        if (wallet != null) {
            wallet.addWalletTransaction(new WalletTransaction(WalletTransaction.Pool.PENDING, tx));
        }

        return true;
    }

    @Override
    public void addTxListener (OnTxCommand executor) {
        txListener.add(executor);
    }

    @Override
    public void addBlockListener (OnBlockCommand executor) {
        blockListener.add(executor);
    }

    @Override
    public Transaction getTransaction (Sha256Hash hash) {
        return null;
    }

    @Override
    public int getHeight () {
        return height;
    }
}
