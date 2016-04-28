package network.thunder.core;

import com.google.gson.Gson;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.InMemoryDBHandler;
import network.thunder.core.etc.Configuration;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.ResultCommandExt;
import network.thunder.core.helper.wallet.MockWallet;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Wallet;

import java.io.BufferedReader;
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
 * Created by matsjerratsch on 11/02/2016.
 */
public class MainNode {

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
            configuration.publicKey = key.getPublicKeyAsHex();
            newConfiguration = true;
            askForHostname(configuration);
        }
        ServerObject server = new ServerObject();
        server.portServer = configuration.portServer;
        server.hostServer = configuration.hostnameServer;
        server.pubKeyServer = ECKey.fromPrivate(Tools.hexStringToByteArray(configuration.serverKey));

        List<byte[]> nodesToBuildChannelWith = new ArrayList<>();
        for (String s : configuration.nodesToBuildChannelWith) {
            nodesToBuildChannelWith.add(Tools.hexStringToByteArray(s));
        }

        DBHandler dbHandler = new InMemoryDBHandler();
        Wallet wallet = new MockWallet(Constants.getNetwork());

        ThunderContext context = new ThunderContext(wallet, dbHandler, server);

        ResultCommandExt listener = new ResultCommandExt();
        context.startListening(listener);
        listener.await();

        boolean successful = false;
        while (!successful) {
            ResultCommandExt fetchNetworkListener = new ResultCommandExt();
            context.fetchNetworkIPs(fetchNetworkListener);
            successful = fetchNetworkListener.await().wasSuccessful();
        }

        ResultCommandExt buildChannelListener = new ResultCommandExt();
        if (newConfiguration) {
            List<String> channelList = showIntroductionAndGetNodeList(server, dbHandler);

            if (channelList == null) {
                context.createRandomChannels(buildChannelListener);
                buildChannelListener.await(30, TimeUnit.SECONDS);
            } else {
                configuration.nodesToBuildChannelWith.addAll(channelList);
                buildConnection(context, configuration);
            }
        } else {
            buildConnection(context, configuration);
        }

        if (newConfiguration) {
            List<Channel> openChannel = dbHandler.getOpenChannel();
            for (Channel channel : openChannel) {
                configuration.nodesToBuildChannelWith.add(Tools.bytesToHex(channel.nodeKeyClient));
            }
            Path file = Paths.get(CONFIG_FILE);
            String config = new Gson().toJson(configuration);
            System.out.println(config);
            Files.write(file, config.getBytes(), StandardOpenOption.CREATE_NEW);
        }

        while (true) {
            Thread.sleep(1000);
        }
    }

    static void buildConnection (ThunderContext context, Configuration configuration) {
        for (String s : configuration.nodesToBuildChannelWith) {
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
            } else {

                ECKey key = null;
                try {
                    key = ECKey.fromPublicOnly(Tools.hexStringToByteArray(s));
                } catch (Exception e) {
                }

                if (key == null) {
                    System.out.println("Invalid pubkey..");
                } else {
                    System.out.println(key.getPublicKeyAsHex() + " added to list..");
                    nodeList.add(s);
                }
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
