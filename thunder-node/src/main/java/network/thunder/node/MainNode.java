package network.thunder.node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.thunder.core.ThunderContext;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.inmemory.InMemoryDBHandler;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.ResultCommandExt;
import network.thunder.core.helper.wallet.MockWallet;
import network.thunder.node.etc.Configuration;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * This is the entry point for starting a thunder.network node.
 * <p>
 * On startup it tries to read from `config.json`, if that fails it creates a new configuration with a new private nodekey.
 */
public class MainNode {
    private static final Logger log = Tools.getLogger();

    static final String CONFIG_FILE = "config.json";

    public static void main (String[] args) throws Exception {
        boolean newConfiguration = false;
        Configuration configuration;
        try {
            String config = readFile(CONFIG_FILE, Charset.defaultCharset());
            configuration = new Gson().fromJson(config, Configuration.class);
        } catch (Exception e) {
            //No configuration supplied - lets create a new one..
            configuration = new Configuration();
            configuration.portServer = Constants.STANDARD_PORT;
            //Use external hostname for now as a hack..
            configuration.hostnameServer = getExternalIP();
            ECKey key = new ECKey();
            configuration.serverKey = Tools.bytesToHex(key.getPrivKeyBytes());
            // TODO: Verify that the public key corresponds to the private key when loading config from file
            configuration.publicKey = key.getPublicKeyAsHex();
            newConfiguration = true;
            askForHostname(configuration);
        }

        //Let's create a ServerObject that holds the general configuration, it will get passed into each Layer of each Connection later.
        ServerObject server = new ServerObject();
        server.portServer = configuration.portServer;
        server.hostServer = configuration.hostnameServer;
        server.pubKeyServer = ECKey.fromPrivate(Tools.hexStringToByteArray(configuration.serverKey));

        //Currently we are only using an in-memory implementation of the DBHandler and a Wallet that is not holding real bitcoin
        DBHandler dbHandler = new InMemoryDBHandler();

        //Setup bitcoin testnet wallet
        Wallet wallet = setupWallet();

        ThunderContext context = new ThunderContext(wallet, dbHandler, server);

        //Start listening on port specified in the configuration
        startListening(context);
        Thread.sleep(1000);

        //Fetch IPs from other participants in the network
        fetchNetworkIPs(context);
        Thread.sleep(5000);

        //Finally build payment channels
        if (!newConfiguration) {
            //Known configuration
            buildPaymentChannels(context, configuration);
        } else {
            //New configuration, ask the user which nodes he wants to connect to..
            List<String> channelList = showIntroductionAndGetNodeList(server, dbHandler);

            if (channelList == null) {
                ResultCommandExt buildChannelListener = new ResultCommandExt();
                context.createRandomChannels(buildChannelListener);
                buildChannelListener.await(30, TimeUnit.SECONDS);
                List<Channel> openChannel = dbHandler.getOpenChannel();
                for (Channel channel : openChannel) {
                    configuration.nodesToBuildChannelWith.add(channel.nodeKeyClient.getPubKeyHex());
                }
                writeConfigurationFile(configuration);
            } else {
                configuration.nodesToBuildChannelWith.addAll(channelList);
                writeConfigurationFile(configuration);
                buildPaymentChannels(context, configuration);
            }
        }
    }

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
            wallet.addCoinsReceivedEventListener((Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) -> {
                log.info("wallet = " + w);
                log.info("tx = " + tx);
            });
            wallet.addCoinsSentEventListener((Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) -> {
                log.info("wallet = " + wallet);
                log.info("tx = " + tx);
            });
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    walletAppKit.stopAsync().awaitTerminated();
                } catch (Exception e) {
                    log.warn("", e);
                }
            }));
            return wallet;
        }
    }

    static void writeConfigurationFile (Configuration configuration) throws IOException {
        Path file = Paths.get(CONFIG_FILE);
        String config = new GsonBuilder().setPrettyPrinting().create().toJson(configuration);
        log.info(config);
        Files.write(file, config.getBytes(), StandardOpenOption.CREATE_NEW);
    }

    static void startListening (ThunderContext context) {
        ResultCommandExt listener = new ResultCommandExt();
        context.startListening(listener);
        listener.await();
        log.info("MainNode.startListening");
    }

    static void fetchNetworkIPs (ThunderContext context) {
        boolean successful = false;
        while (!successful) {
            ResultCommandExt fetchNetworkListener = new ResultCommandExt();
            context.fetchNetworkIPs(fetchNetworkListener);
            successful = fetchNetworkListener.await().wasSuccessful();
        }
    }

    static void buildPaymentChannels (ThunderContext context, Configuration configuration) {
        for (String s : configuration.nodesToBuildChannelWith) {
            log.info("MainNode.buildPaymentChannels " + s);
            ResultCommandExt buildChannelListener = new ResultCommandExt();
            byte[] nodeKey = Tools.hexStringToByteArray(s);
            context.openChannel(nodeKey, buildChannelListener);
            buildChannelListener.await(10, TimeUnit.SECONDS);
        }
    }

    static void askForHostname (Configuration configuration) {
        System.out.println("Specify your hostname (or leave blank to use " + configuration.hostnameServer + ")");
        System.out.print(">>>> ");
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        if (!s.equals("")) {
            configuration.hostnameServer = s;
        }
    }

    static List<String> showIntroductionAndGetNodeList (ServerObject server, DBHandler dbHandler) {
        System.out.println("Thunder.Wallet NodeKey");
        System.out.println("Your public key is:     " + server.pubKeyServer.getPublicKeyAsHex());
        System.out.println();
        System.out.println("Nodes currently online: ");

        for (PubkeyIPObject ipObject : dbHandler.getIPObjects()) {
            System.out.format("%10s %-50s %48s%n", "", ipObject.hostname + ":" + ipObject.port, Tools.bytesToHex(ipObject.pubkey));
        }
        System.out.println();
        System.out.println("Choose pubkeys of nodes to connect to (random for complete random, empty when done)");

        List<String> nodeList = new ArrayList<>();
        while (true) {
            System.out.print(">>>> ");
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();

            if (s.equals("0")) {
                return null;
            } else if (s.equals("")) {
                return nodeList;
            } else if (s.equals(server.pubKeyServer.getPublicKeyAsHex())) {
                System.out.println("You cannot connect to yourself..");
            } else if (nodeList.contains(s)) {
                System.out.println("Pubkey already added to the list..");
            } else {
                ECKey key;
                try {
                    key = ECKey.fromPublicOnly(Tools.hexStringToByteArray(s));
                } catch (Exception e) {
                    System.out.println("Invalid pubkey..");
                    continue;
                }

                System.out.println(key.getPublicKeyAsHex() + " added to list..");
                nodeList.add(s);
            }

        }
    }

    static String getExternalIP () throws IOException {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                whatismyip.openStream()));

        return in.readLine();
    }

    static String readFile (String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
