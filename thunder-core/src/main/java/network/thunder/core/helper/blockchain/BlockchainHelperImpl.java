package network.thunder.core.helper.blockchain;

import network.thunder.core.etc.BlockWrapper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.blockchain.bciapi.BlockExplorer;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletTransaction;
import org.slf4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * BlockchainHelper based upon connecting directly to p2p network with bitcoinJ.
 * Using BC.i for getTransaction, as this functionality is not generally accessible normally.
 * <p>
 * Would be great to have another implementation in the future that only connects to a local bitcoind,
 * to greatly improve privacy and reduce dependency on third parties.
 */
public class BlockchainHelperImpl implements BlockchainHelper {
    private static final Logger log = Tools.getLogger();

    PeerGroup peerGroup;
    BlockStore blockStore;
    BlockChain blockChain;

    Set<Sha256Hash> processedMessages = new HashSet<>();

    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(8, 8, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    List<OnBlockCommand> blockListener = Collections.synchronizedList(new ArrayList<>());
    List<OnTxCommand> txListener = Collections.synchronizedList(new ArrayList<>());

    BlockExplorer blockExplorer = new BlockExplorer();

    Boolean initialized = new Boolean(false);
    Wallet wallet;

    public BlockchainHelperImpl () {
        init();
    }

    public BlockchainHelperImpl (Wallet wallet) {
        init();
        this.wallet = wallet;
    }

    @Override
    public boolean broadcastTransaction (Transaction tx) {
        log.debug("Broadcast transaction: {1}", tx);
        try {
            TransactionBroadcast broadcast = peerGroup.broadcastTransaction(tx);
            broadcast.future().get(10, TimeUnit.SECONDS);
            log.info(tx.getHash() + " broadcasted successfully!");
            wallet.addWalletTransaction(new WalletTransaction(WalletTransaction.Pool.PENDING, tx));
            return true;
        } catch (Exception e) {
            log.debug("e.getMessage() = " + e.getMessage());
        }
        return false;
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
        try {
            Transaction transaction = blockExplorer.getBitcoinJTransaction(hash.toString());
            double height = blockExplorer.getTransaction(hash.toString()).getBlockHeight();
            if (height > 0) {
                transaction.getConfidence().setDepthInBlocks((int) (blockChain.getChainHead().getHeight() - height));
            }
            return transaction;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BlockWrapper getBlock (Sha256Hash hash) {
        try {
            int height = blockStore.get(hash).getHeight();
            Block block = peerGroup.getDownloadPeer().getBlock(hash).get();
            return new BlockWrapper(block, height);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<BlockWrapper> getBlocksSince (int blockHeight) {
        try {
            StoredBlock chainHead = blockStore.getChainHead();
            List<BlockWrapper> blockWrapperList = new ArrayList<>();
            do {
                blockWrapperList.add(getBlock(chainHead.getHeader().getHash()));
                chainHead = blockStore.get(chainHead.getHeader().getPrevBlockHash());
            } while (chainHead.getHeight() > blockHeight);
            blockWrapperList.sort((b1, b2) -> (b1.height - b2.height));
            return blockWrapperList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getHeight () {
        return blockChain.getBestChainHeight();
    }

    public void init () {
        synchronized (initialized) {
            if (!initialized) {
                try {
                    blockStore = new SPVBlockStore(Constants.getNetwork(), new File("blockheaders"));
                } catch (Exception e) {
                    blockStore = new MemoryBlockStore(Constants.getNetwork());
                }

                try {
                    blockChain = new BlockChain(Constants.getNetwork(), blockStore);
                    peerGroup = new PeerGroup(Constants.getNetwork(), blockChain);
                    peerGroup.addPeerDiscovery(new DnsDiscovery(Constants.getNetwork()));
                    peerGroup.setDownloadTxDependencies(0);
                    peerGroup.setBloomFilteringEnabled(false);
                    // Setting to less than now - 1 block according to bitcoinj documentation
                    peerGroup.setFastCatchupTimeSecs(System.currentTimeMillis() / 1000 - 7200);
                    peerGroup.start();
                    peerGroup.addPreMessageReceivedEventListener((Peer peer, Message m) -> {
                        if (m instanceof Block || m instanceof Transaction) {
                            if (processedMessages.add(m.getHash())) {
                                poolExecutor.submit((Runnable) () -> {
                                    try {
                                        if (m instanceof Block) {
                                            for (OnBlockCommand onBlockCommand : blockListener) {
                                                Block block = (Block) m;
                                                onBlockCommand.execute(new BlockWrapper(block, blockStore.get(block.getHash()).getHeight()));
                                            }
                                        } else {
                                            Transaction transaction = (Transaction) m;
                                            Iterator<OnTxCommand> iterator = txListener.iterator();
                                            while (iterator.hasNext()) {
                                                OnTxCommand onTxCommand = iterator.next();
                                                if (onTxCommand.compare(transaction)) {
                                                    onTxCommand.execute(transaction);
                                                    iterator.remove();
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("", e);
                                    }
                                });
                            }
                        }
                        return m;
                    });

                    registerShutdownHook();

                    peerGroup.startBlockChainDownload(new DownloadProgressTracker());

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                initialized = true;
            }
        }
    }

    private void registerShutdownHook () {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (BlockStoreException e) {
                log.warn("", e);
            }
        }));
    }

    public void shutdown () throws BlockStoreException {
        peerGroup.stop();
        blockStore.close();
    }

}
