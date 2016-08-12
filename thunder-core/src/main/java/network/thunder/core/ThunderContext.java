package network.thunder.core;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.ContextFactoryImpl;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.*;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.processor.ConnectionIntent;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.PaymentRequest;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.ConnectionListener;
import network.thunder.core.helper.callback.ResultCommand;
import network.thunder.core.helper.callback.SyncListener;
import network.thunder.core.helper.callback.results.FailureResult;
import network.thunder.core.helper.callback.results.Result;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.events.LNEventHelperImpl;
import network.thunder.core.helper.events.LNEventListener;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class ThunderContext {
    private static final Logger log = Tools.getLogger();

    Wallet wallet;
    DBHandler dbHandler;
    ServerObject node;

    LNEventHelper eventHelper;
    ContextFactory contextFactory;

    LNConfiguration configuration = new LNConfiguration();

    ExecutorService executorService = new ThreadPoolExecutor(1, 4, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

    public ThunderContext (Wallet wallet, DBHandler dbHandler, ServerObject node) {
        this.wallet = wallet;
        this.dbHandler = dbHandler;
        this.node = node;

        init();
    }

    private void init () {
        eventHelper = new LNEventHelperImpl();
        contextFactory = new ContextFactoryImpl(node, dbHandler, wallet, eventHelper);
    }

    public void startUp (ResultCommand resultCallback) {
        contextFactory.getChannelManager().setup();
        startListening(
                result -> fetchNetworkIPs(
                        result1 -> {
                            if (result1.wasSuccessful()) {
                                getSyncData(new SyncListener());
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
        contextFactory.getConnectionManager().startListening(resultCallback);
    }

    public void openChannel (byte[] node, ResultCommand resultCallback) {
        contextFactory.getChannelManager().openChannel(new NodeKey(node),
                new ChannelOpenListener() {
                    @Override
                    public void onFinished (Result result) {
                        resultCallback.execute(result);
                        contextFactory.getSyncHelper().resync(new SyncListener());
                    }
                });
    }

    public void makePayment (byte[] receiver, long amount, PaymentSecret secret, ResultCommand resultCallback) {
        try {
            if (Arrays.equals(receiver, node.pubKeyServer.getPubKey())) {
                throw new LNPaymentException("Can't send to yourself!");
            }

            LNOnionHelper onionHelper = contextFactory.getOnionHelper();
            LNRoutingHelper routingHelper = contextFactory.getLNRoutingHelper();

            List<ChannelStatusObject> route = routingHelper.getRoute(node.pubKeyServer.getPubKey(), receiver, 1000L, 1f, 1f, 1f);
            if (route.size() == 0) {
                throw new LNPaymentException("No route found..");
            }

            LNPaymentHelper paymentHelper = contextFactory.getPaymentHelper();

            PaymentData paymentData = new PaymentData();
            paymentData.amount = amount;
            paymentData.secret = secret;
            paymentData.sending = true;
            paymentData.timestampOpen = Tools.currentTime();
            paymentData.timestampRefund = Tools.currentTime() + route.size()
                    * configuration.MAX_REFUND_DELAY * configuration.MAX_OVERLAY_REFUND;

            paymentData.onionObject = onionHelper.createOnionObject(route, new NodeKey(receiver), paymentData.timestampRefund, amount, null);

            paymentHelper.makePayment(paymentData);
        } catch (Exception e) {
            e.printStackTrace();
            resultCallback.execute(new FailureResult(e.getMessage()));
        }
    }

    public PaymentRequest receivePayment (long amount) {
        PaymentSecret secret = new PaymentSecret(Tools.getRandomByte(20));
        dbHandler.addPaymentSecret(secret);
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.amount = amount;
        paymentRequest.paymentSecret = secret;
        paymentRequest.pubkey = node.pubKeyServer.getPubKey();
        return paymentRequest;
    }

    public void closeChannel (Channel channel, ResultCommand resultCommand) {
        contextFactory.getChannelManager().closeChannel(channel, resultCommand);
    }

    public void fetchNetworkIPs (ResultCommand resultCallback) {
        contextFactory.getConnectionManager().fetchNetworkIPs(resultCallback);
    }

    public Future getSyncData (SyncListener syncListener) {
        log.debug("ThunderContext.getSyncData");
        return executorService.submit(new Runnable() {
            @Override
            public void run () {
                try {
                    contextFactory.getConnectionManager().randomConnections(2, ConnectionIntent.GET_SYNC_DATA, new ConnectionListener()).get();
                    contextFactory.getSyncHelper().resync(syncListener).get();
                    contextFactory.getConnectionManager().disconnectByIntent(ConnectionIntent.GET_SYNC_DATA);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void createRandomChannels (ResultCommand resultCallback) {
        new Thread(new Runnable() {
            @Override
            public void run () {
                try {
                    contextFactory.getConnectionManager().startBuildingRandomChannel(resultCallback);
                } catch (Exception e) {
                    log.warn("", e);
                }
            }
        }).start();
    }

}
