package network.thunder.core;

import com.google.gson.Gson;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ResultCommandExt;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.etc.*;
import network.thunder.core.mesh.NodeServer;
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
import java.util.concurrent.TimeUnit;

/**
 * Created by matsjerratsch on 11/02/2016.
 */
public class Main {

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
            configuration.portServer = 10000;
            //Use external IP for now as a hack..
            configuration.hostnameServer = getExternalIP();
            configuration.serverKey = Tools.bytesToHex(new ECKey().getPrivKeyBytes());
            newConfiguration = true;
        }
        NodeServer server = new NodeServer();
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
        if (configuration.nodesToBuildChannelWith.size() == 0) {
            context.createRandomChannels(buildChannelListener);
            buildChannelListener.await(30, TimeUnit.SECONDS);
        } else {
            for (String s : configuration.nodesToBuildChannelWith) {
                buildChannelListener = new ResultCommandExt();
                byte[] nodeKey = Tools.hexStringToByteArray(s);
                context.openChannel(nodeKey, buildChannelListener);
                buildChannelListener.await(10, TimeUnit.SECONDS);
            }
        }

        if (newConfiguration) {
            List<Channel> openChannel = dbHandler.getOpenChannel();
            for (Channel channel : openChannel) {
                configuration.nodesToBuildChannelWith.add(Tools.bytesToHex(channel.nodeId));
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
