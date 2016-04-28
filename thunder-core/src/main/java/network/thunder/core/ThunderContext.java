package network.thunder.core;

import network.thunder.core.communication.*;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.ContextFactoryImpl;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.*;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.ResultCommand;
import network.thunder.core.helper.callback.results.FailureResult;
import network.thunder.core.helper.callback.results.NullResultCommand;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.events.LNEventHelperImpl;
import network.thunder.core.helper.events.LNEventListener;
import org.bitcoinj.core.Wallet;

import java.util.Arrays;
import java.util.List;

public class ThunderContext {

    Wallet wallet;
    DBHandler dbHandler;
    ServerObject node;

    LNEventHelper eventHelper;
    ContextFactory contextFactory;

    ConnectionManager connectionManager;

    LNConfiguration configuration = new LNConfiguration();

    public ThunderContext (Wallet wallet, DBHandler dbHandler, ServerObject node) {
        this.wallet = wallet;
        this.dbHandler = dbHandler;
        this.node = node;

        init();
    }

    private void init () {
        eventHelper = new LNEventHelperImpl();
        contextFactory = new ContextFactoryImpl(node, dbHandler, wallet, eventHelper);
        connectionManager = new ConnectionManagerImpl(contextFactory, dbHandler);
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
        contextFactory.getSyncHelper().resync();
        contextFactory.getChannelManager().openChannel(new NodeKey(node), new ChannelOpenListener());
//        connectionManager.buildChannel(node, resultCallback);
    }

    public void makePayment (byte[] receiver, long amount, PaymentSecret secret, ResultCommand resultCallback) {
        try {
            if (Arrays.equals(receiver, node.pubKeyServer.getPubKey())) {
                throw new LNPaymentException("Can't send to yourself!");
            }
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

    public void closeChannel (Channel channel, ResultCommand resultCommand) {
        contextFactory.getChannelManager().closeChannel(channel, resultCommand);
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
