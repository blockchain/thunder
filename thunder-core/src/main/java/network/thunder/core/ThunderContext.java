package network.thunder.core;

import network.thunder.core.communication.nio.ConnectionManager;
import network.thunder.core.communication.nio.ConnectionManagerImpl;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.LNEventHelperImpl;
import network.thunder.core.communication.objects.messages.impl.factories.ContextFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.objects.messages.impl.results.FailureResult;
import network.thunder.core.communication.objects.messages.impl.results.NullResultCommand;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.*;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ResultCommand;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.LNConfiguration;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.Wallet;

import java.util.List;

/**
 * Created by matsjerratsch on 08/02/2016.
 */
public class ThunderContext {

    Wallet wallet;
    DBHandler dbHandler;
    NodeServer node;

    LNEventHelper eventHelper;
    ContextFactory contextFactory;

    ConnectionManager connectionManager;

    LNConfiguration configuration = new LNConfiguration();

    public ThunderContext (Wallet wallet, DBHandler dbHandler, NodeServer node) {
        this.wallet = wallet;
        this.dbHandler = dbHandler;
        this.node = node;

        init();
    }

    private void init () {
        eventHelper = new LNEventHelperImpl();
        contextFactory = new ContextFactoryImpl(node, dbHandler, wallet, eventHelper);
        connectionManager = new ConnectionManagerImpl(node, contextFactory, dbHandler, eventHelper);
    }

    public void startUp (ResultCommand resultCallback) {
        startListening(
                result -> fetchNetworkIPs(
                        result1 -> {
                            if (result1.wasSuccessful()) {
                                getSyncData(new NullResultCommand());
                            }
                        }));
    }

    public void addEventListener (LNEventListener eventListener) {
        eventHelper.addListener(eventListener);
    }

    public void removeEventListener (LNEventListener eventListener) {
        eventHelper.removeListener(eventListener);
    }

    public void startListening (ResultCommand resultCallback) {
        connectionManager.startListening(resultCallback);
    }

    public void openChannel (byte[] node, ResultCommand resultCallback) {
        connectionManager.buildChannel(node, resultCallback);
    }

    public void makePayment (byte[] receiver, long amount, PaymentSecret secret, ResultCommand resultCallback) {
        try {
            LNPaymentHelper paymentHelper = contextFactory.getPaymentHelper();
            LNOnionHelper onionHelper = contextFactory.getOnionHelper();
            LNRoutingHelper routingHelper = contextFactory.getLNRoutingHelper();

            List<byte[]> route = routingHelper.getRoute(node.pubKeyServer.getPubKey(), receiver, 1000L, 1f, 1f, 1f);

            OnionObject object = onionHelper.createOnionObject(route, null);

            PaymentData paymentData = new PaymentData();
            paymentData.amount = amount;
            paymentData.onionObject = object;
            paymentData.sending = true;
            paymentData.secret = secret;
            paymentData.timestampOpen = Tools.currentTime();
            paymentData.timestampRefund = Tools.currentTime() + route.size()
                    * configuration.MAX_REFUND_DELAY * configuration.MAX_OVERLAY_REFUND;
            paymentData.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;

            paymentHelper.makePayment(paymentData);
        } catch (Exception e) {
            resultCallback.execute(new FailureResult(e.getMessage()));
        }
    }

    public void fetchNetworkIPs (ResultCommand resultCallback) {
        connectionManager.fetchNetworkIPs(resultCallback);
    }

    public void getSyncData (ResultCommand resultCallback) {
        contextFactory.getSyncHelper().resync();
        connectionManager.startSyncing(resultCallback);
    }

    public void createRandomChannels (ResultCommand resultCallback) {
        new Thread(new Runnable() {
            @Override
            public void run () {
                try {
                    connectionManager.startBuildingRandomChannel(resultCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
