/*
 *  ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 *  Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package network.thunder.server.wallet;

import com.lambdaworks.codec.Base64;
import network.thunder.server.database.MySQLConnection;
import network.thunder.server.database.objects.Output;
import network.thunder.server.etc.Constants;
import network.thunder.server.etc.SideConstants;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO: Auto-generated Javadoc

/**
 * The Class KeyChain.
 */
public class KeyChain {

    public WalletAppKit kit;
    public Wallet wallet;
    public PeerGroup peerGroup;
    public PeerGroup peerGroup2;
    public Connection conn;
    /**
     * The key.
     */
    String KEY = "episode slice essence biology cream broccoli agree poverty sentence piano eyebrow air";
    private boolean isRunning = false;
    private ArrayList<Transaction> checkTransactionList = new ArrayList<>();
    private ArrayList<Transaction> confidenceChangedTransactionList = new ArrayList<>();

    /**
     * Instantiates a new key chain.
     *
     * @param conn the conn
     * @throws Exception the exception
     */
    public KeyChain (Connection conn) throws Exception {
        this.conn = conn;
    }

    public void run () throws Exception {

        MySQLConnection.deleteAllOutputs(conn);
        for (TransactionOutput o : wallet.calculateAllSpendCandidates()) {
            if (o.getValue().value == 0) {
                continue;
            }
            System.out.println(o.getParentTransaction().getConfidence().getDepthInBlocks() + " " + o);
            try {
                TransactionStorage.instance.onTransaction(o.getParentTransaction());
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            if (o.getParentTransaction().getConfidence().getDepthInBlocks() >= Constants.MIN_CONFIRMATION_TIME) {
                try {
                    Output output = new Output();
                    output.setVout(o.getIndex());
                    output.setHash(o.getParentTransaction().getHash().toString());
                    output.setValue(o.getValue().value);
                    output.setPrivateKey(new String(Base64.encode(wallet.findKeyFromPubHash(o.getAddressFromP2PKHScript(Constants.getNetwork()).getHash160()).getPrivKeyBytes())));
                    output.setTransactionOutput(o);
                    MySQLConnection.addOutput(conn, output);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (Transaction tx : checkTransactionList) {
            TransactionStorage.instance.onTransaction(tx);
        }
        for (Transaction tx : confidenceChangedTransactionList) {
            TransactionStorage.instance.onConfidenceChanged(tx);
        }

    }

    public void shutdown () {
        kit.stopAndWait();
    }

    /**
     * Start.
     *
     * @throws Exception the exception
     */
    public void startUp () throws Exception {

        kit = new WalletAppKit(Constants.getNetwork(), new File("."), SideConstants.WALLET_FILE);
        kit.setAutoSave(true);
        kit.startAndWait();
        //        kit.awaitRunning();
        //      kit.wallet().reset();
        peerGroup = kit.peerGroup();
        wallet = kit.wallet();

        /**
         * TODO: Can't get it working with the peergroup within the WalletAppKit, use a new one for now..
         * Somehow, onTransaction does not get called on the WalletAppKit peerGroup Listener..
         */
        peerGroup2 = new PeerGroup(Constants.getNetwork(), null /* no chain */);
        peerGroup2.setUserAgent("PeerMonitor", "1.0");
        peerGroup2.setMaxConnections(4);
        peerGroup2.addPeerDiscovery(new DnsDiscovery(Constants.getNetwork()));
        peerGroup2.addEventListener(new PeerListener());
        peerGroup2.start();

        kit.wallet().addEventListener(new WalletListener());
        System.out.println(kit.wallet().toString(false, false, false, null));

    }

    /**
     * The listener interface for receiving wallet events.
     * The class that is interested in processing a wallet
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addWalletListener<code> method. When
     * the wallet event occurs, that object's appropriate
     * method is invoked.
     */
    class WalletListener extends AbstractWalletEventListener {

        @Override
        public void onCoinsReceived (Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            System.out.println("-----> coins resceived: " + tx.getHashAsString());
            System.out.println("received: " + tx.getValue(wallet));
            System.out.println("send money to: " + kit.wallet().freshReceiveAddress().toString());
            try {
                if (isRunning) {
                    TransactionStorage.instance.onTransaction(tx);
                } else {
                    checkTransactionList.add(tx);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.AbstractWalletEventListener#onCoinsSent(org.bitcoinj.core.Wallet, org.bitcoinj.core.Transaction, org.bitcoinj.core.Coin, org
         * .bitcoinj.core.Coin)
         */
        @Override
        public void onCoinsSent (Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.AbstractWalletEventListener#onReorganize(org.bitcoinj.core.Wallet)
         */
        @Override
        public void onReorganize (Wallet wallet) {
        }

        @Override
        public void onTransactionConfidenceChanged (Wallet wallet, Transaction tx) {

            try {
                if (isRunning) {
                    TransactionStorage.instance.onConfidenceChanged(tx);
                } else {
                    confidenceChangedTransactionList.add(tx);
                }
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            TransactionConfidence confidence = tx.getConfidence();

            if (confidence.getDepthInBlocks() < 2 && confidence.getDepthInBlocks() > 2) {
                System.out.println("-----> confidence changed: " + tx.getHashAsString());
                System.out.println("new block depth: " + confidence.getDepthInBlocks());
            }

            if (confidence.getDepthInBlocks() == Constants.MIN_CONFIRMATION_TIME) {
                for (TransactionOutput o : tx.getOutputs()) {
                    try {
                        Output output = new Output();
                        output.setVout(o.getIndex());
                        output.setHash(tx.getHashAsString());
                        output.setValue(o.getValue().value);
                        output.setPrivateKey(new String(Base64.encode(wallet.findKeyFromPubHash(o.getAddressFromP2PKHScript(Constants.getNetwork()).getHash160()).getPrivKeyBytes())));
                        output.setTransactionOutput(o);

                        MySQLConnection.addOutput(conn, output);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.AbstractWalletEventListener#onKeysAdded(java.util.List)
         */
        @Override
        public void onKeysAdded (List<ECKey> keys) {
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.AbstractWalletEventListener#onWalletChanged(org.bitcoinj.core.Wallet)
         */
        @Override
        public void onWalletChanged (Wallet wallet) {
        }

    }

    /**
     * The listener interface for receiving peer events.
     * The class that is interested in processing a peer
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addPeerListener<code> method. When
     * the peer event occurs, that object's appropriate
     * method is invoked.
     */
    class PeerListener implements PeerEventListener {

        /* (non-Javadoc)
         * @see org.bitcoinj.core.PeerEventListener#onPeersDiscovered(java.util.Set)
         */
        public void onPeersDiscovered (Set<PeerAddress> peerAddresses) {
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.PeerEventListener#onBlocksDownloaded(org.bitcoinj.core.Peer, org.bitcoinj.core.Block, org.bitcoinj.core.FilteredBlock, int)
         */
        public void onBlocksDownloaded (Peer peer, Block block, FilteredBlock filteredBlock, int blocksLeft) {
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.PeerEventListener#onChainDownloadStarted(org.bitcoinj.core.Peer, int)
         */
        public void onChainDownloadStarted (Peer peer, int blocksLeft) {

        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.PeerEventListener#onPeerConnected(org.bitcoinj.core.Peer, int)
         */
        public void onPeerConnected (Peer peer, int peerCount) {
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.PeerEventListener#onPeerDisconnected(org.bitcoinj.core.Peer, int)
         */
        public void onPeerDisconnected (Peer peer, int peerCount) {
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.PeerEventListener#onPreMessageReceived(org.bitcoinj.core.Peer, org.bitcoinj.core.Message)
         */
        public Message onPreMessageReceived (Peer peer, Message m) {
            return m;
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.PeerEventListener#onTransaction(org.bitcoinj.core.Peer, org.bitcoinj.core.Transaction)
         */
        public void onTransaction (Peer peer, Transaction tx) {
            try {
                if (isRunning) {
                    TransactionStorage.instance.onTransaction(tx);
                } else {
                    checkTransactionList.add(tx);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /* (non-Javadoc)
         * @see org.bitcoinj.core.PeerEventListener#getData(org.bitcoinj.core.Peer, org.bitcoinj.core.GetDataMessage)
         */
        public List<Message> getData (Peer peer, GetDataMessage m) {
            return null;
        }

    }

}
