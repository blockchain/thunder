package network.thunder.node;

import com.google.gson.Gson;
import network.thunder.core.communication.ConnectionManager;
import network.thunder.core.communication.ConnectionManagerImpl;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.ContextFactoryImpl;
import network.thunder.core.communication.layer.high.payments.LNOnionHelper;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.inmemory.InMemoryDBHandler;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.results.NullResultCommand;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.events.LNEventHelperImpl;
import network.thunder.core.helper.wallet.MockWallet;
import network.thunder.node.etc.Configuration;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class MainClient {

    public static void main (String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        String config = readFile("config.json", Charset.defaultCharset());

        Configuration configuration = new Gson().fromJson(config, Configuration.class);

        ServerObject server = new ServerObject();
        server.portServer = configuration.portServer;
        server.hostServer = configuration.hostnameServer;
//        server.pubKeyServer = ECKey.fromPrivate(Tools.hexStringToByteArray(configuration.serverKey));
        server.pubKeyServer = new ECKey();

        List<byte[]> nodesToBuildChannelWith = new ArrayList<>();
        for (String s : configuration.nodesToBuildChannelWith) {
            nodesToBuildChannelWith.add(Tools.hexStringToByteArray(s));
        }

        DBHandler dbHandler = new InMemoryDBHandler();
        Wallet wallet = new MockWallet(Constants.getNetwork());

        LNEventHelper eventHelper = new LNEventHelperImpl();
        ContextFactory contextFactory = new ContextFactoryImpl(server, dbHandler, wallet, eventHelper);

        ConnectionManager connectionManager = new ConnectionManagerImpl(contextFactory, dbHandler);

        connectionManager.startListening(new NullResultCommand());

        Thread.sleep(1000);

        connectionManager.fetchNetworkIPs(new NullResultCommand());

        for (String s : configuration.nodesToBuildChannelWith) {
            NodeKey nodeKey = new NodeKey(Tools.hexStringToByteArray(s));
            contextFactory.getChannelManager().openChannel(nodeKey, new ChannelOpenListener());
            Thread.sleep(1000);
        }

        //Assume that we have working connections here..
        LNOnionHelper onionHelper = contextFactory.getOnionHelper();

        List<byte[]> route = new ArrayList<>();
        route.add(server.pubKeyServer.getPubKey());

        route.add(Tools.hexStringToByteArray("0392af24ee17b39bf50a61fd679d1c50585d5751d1ca221e91c1535e34d85c50ea"));
        route.add(Tools.hexStringToByteArray("02b247ef9a0e23da6f5db858a48a839242a596ff972f55ecc7fa42e3003e7b713b"));
        route.add(Tools.hexStringToByteArray("021d381f233a902f1572a8c62b58d2b7108cb6292d065861fed1c6eb54e8483b23"));

        route.add(server.pubKeyServer.getPubKey());

        OnionObject onionObject = onionHelper.createOnionObject(route, null);
        PaymentData paymentData = new PaymentData();
        PaymentSecret paymentSecret = new PaymentSecret(Tools.getRandomByte(32));

        dbHandler.addPaymentSecret(paymentSecret);

        paymentData.sending = true;
        paymentData.secret = paymentSecret;
        paymentData.amount = 100;
        paymentData.onionObject = onionObject;

        LNPaymentHelper paymentHelper = contextFactory.getPaymentHelper();
        paymentHelper.makePayment(paymentData);

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
