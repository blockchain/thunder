package network.thunder.core;

import com.google.gson.Gson;
import network.thunder.core.communication.nio.ConnectionManager;
import network.thunder.core.communication.nio.ConnectionManagerImpl;
import network.thunder.core.communication.objects.messages.impl.LNEventHelperImpl;
import network.thunder.core.communication.objects.messages.impl.factories.ContextFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.results.NullResultCommand;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.*;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Wallet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 11/02/2016.
 */
public class Main {

    public static void main (String[] args) throws Exception {
        String config = readFile("config.json", Charset.defaultCharset());

        Configuration configuration = new Gson().fromJson(config, Configuration.class);

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

        LNEventHelper eventHelper = new LNEventHelperImpl();
        ContextFactory contextFactory = new ContextFactoryImpl(server, dbHandler, wallet, eventHelper);

        ConnectionManager connectionManager = new ConnectionManagerImpl(server, contextFactory, dbHandler, eventHelper);

        connectionManager.startListening(new NullResultCommand());

        Thread.sleep(1000);

        connectionManager.fetchNetworkIPs(new NullResultCommand());

        Thread.sleep(2000);

        for (String s : configuration.nodesToBuildChannelWith) {
            byte[] nodeKey = Tools.hexStringToByteArray(s);
            connectionManager.buildChannel(nodeKey, new NullResultCommand());
            Thread.sleep(1000);
        }

        while (true) {
            Thread.sleep(100000);
        }

    }

    static String readFile (String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
