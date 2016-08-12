package network.thunder.node;

import network.thunder.core.ThunderContext;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.persistent.SQLDBHandler;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.PaymentRequest;
import network.thunder.core.helper.callback.ResultCommandExt;
import network.thunder.core.helper.callback.results.NullResultCommand;
import network.thunder.core.helper.wallet.MockWallet;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletEventListener;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainBenchmark {
    private static final Logger log = Tools.getLogger();

    public static void main (String[] args) throws Exception {
        //Currently we are only using an in-memory implementation of the DBHandler and a Wallet that is not holding real bitcoin
        DBHandler dbHandler1 = new SQLDBHandler(Tools.getH2SavedDataSource("db_benchmark_1"));
        DBHandler dbHandler2 = new SQLDBHandler(Tools.getH2SavedDataSource("db_benchmark_2"));

        ServerObject server1 = dbHandler1.getServerObject();
        ServerObject server2 = dbHandler2.getServerObject();

        //Setup bitcoin testnet wallet
        Wallet wallet1 = setupWallet();
        Wallet wallet2 = setupWallet();

        ThunderContext context1 = new ThunderContext(wallet1, dbHandler1, server1);
        ThunderContext context2 = new ThunderContext(wallet2, dbHandler2, server2);

        //Fetch IPs from other participants in the network
        fetchNetworkIPs(context1);
        fetchNetworkIPs(context2);
        Thread.sleep(1000);

        //New configuration, ask the user which nodes he wants to connect to..
        String node = showIntroductionAndGetNodeList(dbHandler1);

        log.info("MainNode.buildPaymentChannels " + node);
        byte[] nodeKey = Tools.hexStringToByteArray(node);
        context1.openChannel(nodeKey, new NullResultCommand());
        Thread.sleep(100);
        context2.openChannel(nodeKey, new NullResultCommand());
        Thread.sleep(2000);

        int TOTAL_PAYMENTS = 1000;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_PAYMENTS; i++) {
            PaymentRequest paymentRequest2 = context2.receivePayment(1);
            context1.makePayment(server2.pubKeyServer.getPubKey(), 1, paymentRequest2.paymentSecret, new NullResultCommand());
        }

        while (true) {
            Channel channel = dbHandler1.getChannel(new NodeKey(nodeKey)).get(0);
            if (channel.amountClient == (10000000 + TOTAL_PAYMENTS)) {

                long endTime = System.currentTimeMillis();

                double diff = (endTime - startTime);

                double tps = TOTAL_PAYMENTS / (diff / 1000);
                log.info("diff = " + diff);
                log.info("tps = " + tps);
                System.exit(0);
            }
            Thread.sleep(100);
        }
    }

    //TODO refactor with MainNode
    static Wallet setupWallet () {
        if (Constants.USE_MOCK_BLOCKCHAIN) {
            return new MockWallet(Constants.getNetwork());
        } else {
            //TODO somehow allow sending money out of the node again..
            log.info("Setting up wallet and downloading blockheaders. This can take up to two minutes on first startup");
            WalletAppKit walletAppKit = new WalletAppKit(Constants.getNetwork(), new File("wallet"), new String("node_"));
            walletAppKit.startAsync().awaitRunning();
            Wallet wallet = walletAppKit.wallet();
            wallet.allowSpendingUnconfirmedTransactions();
            wallet.reset();
            log.info("wallet = " + wallet);
            log.info("wallet.getKeyChainSeed() = " + wallet.getKeyChainSeed());
            wallet.addEventListener(new WalletEventListener() {
                @Override
                public void onCoinsReceived (Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                    log.info("wallet = " + wallet);
                    log.info("tx = " + tx);
                }

                @Override
                public void onCoinsSent (Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                    log.info("wallet = " + wallet);
                    log.info("tx = " + tx);
                }

                @Override
                public void onReorganize (Wallet wallet) {

                }

                @Override
                public void onTransactionConfidenceChanged (Wallet wallet, Transaction tx) {

                }

                @Override
                public void onWalletChanged (Wallet wallet) {
                }

                @Override
                public void onScriptsChanged (Wallet wallet, List<Script> scripts, boolean isAddingScripts) {

                }

                @Override
                public void onKeysAdded (List<ECKey> keys) {

                }
            });
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run () {
                    try {
                        walletAppKit.stopAsync().awaitTerminated();
                    } catch (Exception e) {
                        log.warn("", e);
                    }
                }
            }));
            return wallet;
        }
    }

    static void fetchNetworkIPs (ThunderContext context) {
        boolean successful = false;
        while (!successful) {
            ResultCommandExt fetchNetworkListener = new ResultCommandExt();
            context.fetchNetworkIPs(fetchNetworkListener);
            successful = fetchNetworkListener.await().wasSuccessful();
        }
    }

    static String showIntroductionAndGetNodeList (DBHandler dbHandler) {
        log.info("Thunder.Network Benchmarking");
        log.info("");
        log.info("Nodes currently online: ");

        List<PubkeyIPObject> ipObjects = dbHandler.getIPObjects();

        for (PubkeyIPObject ipObject : ipObjects) {
            System.out.format("%10s %-50s %48s%n", "", ipObject.hostname + ":" + ipObject.port, Tools.bytesToHex(ipObject.pubkey));
        }

        if (ipObjects.size() == 1) {
            log.info(ipObjects.get(0).toString());
            return Tools.bytesToHex(ipObjects.get(0).pubkey);
        }

        log.info("");
        log.info("Choose pubkeys of node to connect to:");

        List<String> nodeList = new ArrayList<>();
        while (true) {
            System.out.print(">>>> ");
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();

            if (nodeList.contains(s)) {
                log.info("Pubkey already added to the list..");
            } else {
                ECKey key;
                try {
                    key = ECKey.fromPublicOnly(Tools.hexStringToByteArray(s));
                } catch (Exception e) {
                    log.info("Invalid pubkey..");
                    continue;
                }

                log.info(key.getPublicKeyAsHex() + " added to list..");
                return s;
            }

        }
    }
}
