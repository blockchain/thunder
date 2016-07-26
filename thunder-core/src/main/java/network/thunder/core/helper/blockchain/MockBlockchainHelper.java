package network.thunder.core.helper.blockchain;

import network.thunder.core.etc.BlockWrapper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletTransaction;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MockBlockchainHelper implements BlockchainHelper {
    private static final Logger log = Tools.getLogger();

    public Set<Sha256Hash> broadcastedTransactionHashes = new HashSet<>();
    public Set<Transaction> broadcastedTransaction = new HashSet<>();

    List<OnBlockCommand> blockListener = Collections.synchronizedList(new ArrayList<>());
    List<OnTxCommand> txListener = Collections.synchronizedList(new ArrayList<>());

    List<Transaction> newTransactions = new ArrayList<>();

    Wallet wallet;

    int height = 0;

    Map<Sha256Hash, Block> blockHashMap = new ConcurrentHashMap<>();
    Map<Sha256Hash, Integer> blockHeightMap = new ConcurrentHashMap<>();
    List<BlockWrapper> blockList = new ArrayList<>();

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

    public List<BlockWrapper> getBlockList () {
        return blockList;
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

        BlockWrapper b = new BlockWrapper(block, height);
        blockList.add(b);
        blockHeightMap.put(block.getHash(), height);
        blockHashMap.put(block.getHash(), block);
        blockListener.forEach(listener -> listener.execute(b));
    }

    @Override
    public boolean broadcastTransaction (Transaction tx) {
        if (broadcastedTransactionHashes.add(tx.getHash())) {
            log.info(" MockBlockchainHelper.broadcastTransaction " + height);
            log.info("tx = " + tx.toString());
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
    public BlockWrapper getBlock (Sha256Hash hash) {
        return new BlockWrapper(blockHashMap.get(hash), blockHeightMap.get(hash));
    }

    @Override
    public List<BlockWrapper> getBlocksSince (int blockHeight) {
        return blockList.stream().filter(b -> b.height > blockHeight).sorted((b1, b2) -> (b1.height - b2.height)).collect(Collectors.toList());
    }

    @Override
    public int getHeight () {
        return height;
    }
}
