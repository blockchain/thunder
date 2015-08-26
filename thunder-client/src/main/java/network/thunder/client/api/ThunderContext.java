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
package network.thunder.client.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import network.thunder.client.communications.WebSocketHandler;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Output;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.communications.ClientTools;
import network.thunder.client.etc.Tools;
import network.thunder.client.wallet.TransactionStorage;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Wallet;


public class ThunderContext {

    public Connection conn;

    private ArrayList<Payment> paymentListIncluded = new ArrayList<Payment>();
    private ArrayList<Payment> paymentListSettled = new ArrayList<Payment>();
    private ArrayList<Payment> paymentListRefunded = new ArrayList<Payment>();
    private ArrayList<Payment> paymentListOpen = new ArrayList<Payment>();

    public ArrayList<Output> outputList = new ArrayList<Output>();

    public HashMap<Integer, Channel> channelList = new HashMap<>();

    public Channel currentChannel;

    private Wallet wallet;
    private PeerGroup peerGroup;

    public TransactionStorage transactionStorage;

    private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
    private InitFinishListener initListener;
    private ProgressUpdateListener updateListener;
    private ErrorListener errorListener;

    private boolean initFinished = false;


    public static ThunderContext instance = new ThunderContext();

    public ThunderContext thisReference;

    public ExecutorService executorService = Executors.newSingleThreadExecutor();
    public WebSocketHandler webSocketHandler = new WebSocketHandler();

    Future latestFuture;


    public static ThunderContext init(Wallet w, PeerGroup p, int clientId, boolean forceNew) throws SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (!forceNew && instance.initFinished)
            return instance;

        ThunderContext context;
        if(!instance.initFinished) {
            context = instance;
        } else {
            context = new ThunderContext();
        }
        context.thisReference = context;
        System.out.println("Start init!");
        context.conn = MySQLConnection.getInstance(clientId);

        /**
         * Hack to check if the database has been created already..
         */
        try {
            context.channelList = MySQLConnection.getActiveChannels(context.conn);
        } catch (SQLException e) {
            MySQLConnection.buildDatabase(context.conn);
        }
        MySQLConnection.buildDatabase(context.conn);


        context.wallet = w;
        context.peerGroup = p;

        context.channelList = MySQLConnection.getActiveChannels(context.conn);
        if (context.channelList.size() > 0) {
            context.currentChannel = context.channelList.entrySet().iterator().next().getValue();
            context.updatePaymentLists();
        }

        context.transactionStorage = TransactionStorage.initialize(context.conn, context.outputList);
        context.wallet.addEventListener(context.transactionStorage);
        System.out.println("Finished init! Active channels: " + context.channelList.size());

        context.transactionStorage.updateOutputs(context.wallet);
        if (context.initListener != null)
            context.initListener.initFinished();

        if (context.currentChannel != null) {
            context.webSocketHandler.connectToServer(context.currentChannel, context);
        }
        context.initFinished = true;
        instance = context;

        return context;
    }

    public static void init(Wallet w, PeerGroup p) throws SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        init(w, p, 1, false);
    }

    private void updatePaymentLists() throws SQLException {
        paymentListIncluded = MySQLConnection.getPaymentsIncludedInChannel(conn, currentChannel.getId());
        paymentListSettled = MySQLConnection.getPaymentsSettled(conn, currentChannel.getId());
        paymentListRefunded = MySQLConnection.getPaymentsRefunded(conn, currentChannel.getId());
        paymentListOpen = MySQLConnection.getPaymentsOpen(conn, currentChannel.getId());
    }


    public ArrayList<Payment> getPaymentListIncluded() {
        return paymentListIncluded;
    }

    public ArrayList<Payment> getPaymentListSettled() {
        return paymentListSettled;
    }

    public ArrayList<Payment> getPaymentListRefunded() {
        return paymentListRefunded;
    }

    public ArrayList<Payment> getPaymentListOpen() {
        return paymentListOpen;
    }


    public HashMap<Integer, Channel> getChannelList() {
        return channelList;
    }

    public Coin getAmountClient() {
        if (currentChannel == null)
            return Coin.ZERO;
        return Coin.valueOf(currentChannel.getAmountClient());
    }

    public Coin getAmountClientAccessible() throws SQLException {
        if (currentChannel == null)
            return Coin.ZERO;
        if (currentChannel.getChannelTxClientID() == 0) {
            return Coin.valueOf(currentChannel.getAmountClient());
        } else {
            return currentChannel.getChannelTxClient().getOutput(0).getValue();
        }
    }

    public boolean hasActiveChannel() {
        System.out.println(channelList);
        System.out.println(channelList.size());
        return (channelList.size() != 0);
    }

    public void addListener(ChangeListener toAdd) {
        System.out.println("Listener added!");
        changeListeners.add(toAdd);
    }

    public void setInitFinishedListener(InitFinishListener listener) {
        initListener = listener;
    }

    public void setProgressUpdateListener(ProgressUpdateListener listener) {
        updateListener = listener;
    }

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    public PaymentRequest getPaymentReceiveRequest(long amount) throws Exception {

        Payment p = new Payment(0, currentChannel.getId(), amount);
        p.setReceiver(currentChannel.getPubKeyClient());
        p.paymentToServer = false;

        MySQLConnection.addPayment(conn, p);
        conn.commit();
        PaymentRequest request = new PaymentRequest(currentChannel, p);

        updatePaymentLists();
        for (ChangeListener listener : changeListeners)
            listener.channelListChanged();

        return request;

    }

    public void openChannel(final long clientAmount, final long serverAmount, final int timeInDays) throws Exception {
        latestFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {

                try {
                    System.out.println("New Thread..");
                    Channel channel = currentChannel;

                    channel = ClientTools.createChannel(conn, wallet, peerGroup, outputList, clientAmount, serverAmount, timeInDays);

                    channelList.put(channel.getId(), channel);
                    currentChannel = channel;

                    webSocketHandler.connectToServer(currentChannel, thisReference);


                    for (ChangeListener listener : changeListeners)
                        listener.channelListChanged();
                } catch (Exception e) {
                    throwError(Tools.stacktraceToString(e));
                    e.printStackTrace();
                }

            }
        });

    }

    public void closeChannel() throws Exception {

        latestFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {

                    ClientTools.closeChannel(conn, currentChannel, peerGroup);
                    channelList.remove(currentChannel.getId());
                    webSocketHandler.closeConnection(currentChannel.getId());

                    if (channelList.size() > 0)
                        currentChannel = channelList.entrySet().iterator().next().getValue();

                    for (ChangeListener listener : changeListeners)
                        listener.channelListChanged();

                } catch (Exception e) {
                    throwError(Tools.stacktraceToString(e));
                    e.printStackTrace();
                }

            }
        });
    }

    public void makePayment(final long amount, final String address) throws Exception {

        PaymentRequest request = new PaymentRequest(currentChannel, amount, address);

        latestFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {


                    currentChannel = ClientTools.makePayment(conn, currentChannel, request.getPayment());
                    updatePaymentLists();
                    for (ChangeListener listener : changeListeners)
                        listener.channelListChanged();

                } catch (Exception e) {
                    e.printStackTrace();
                    try {

                        conn.rollback();
                        MySQLConnection.deletePayment(conn, request.getPayment().getId());
                        conn.commit();
                        currentChannel = MySQLConnection.getChannel(conn, currentChannel.getId());
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }

                    throwError(Tools.stacktraceToString(e));
                }
            }
        });
    }

    public void updateChannel() throws Exception {
        latestFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    /**
                     * TODO: change protocol, such that server sends amount of new payments
                     * 			with the first response, such that we know whether we should update
                     * 			at all.
                     *
                     * TODO: Not sure how this is going to work with multiple channels, we have to check
                     *          back here as soon as we allow more than one active channel.
                     */
                    currentChannel = ClientTools.updateChannel(conn, currentChannel, true);

                    updatePaymentLists();
                    for (ChangeListener listener : changeListeners)
                        listener.channelListChanged();


                    currentChannel = ClientTools.updateChannel(conn, currentChannel, false);

                    updatePaymentLists();
                    for (ChangeListener listener : changeListeners)
                        listener.channelListChanged();

                    ThunderContext.instance.progressUpdated(10, 10);

                } catch (Exception e) {
                    e.printStackTrace();
                    throwError(Tools.stacktraceToString(e));
                    try {
                        conn.rollback();
                        conn.commit();
                        currentChannel = MySQLConnection.getChannel(conn, currentChannel.getId());

                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }

                }
            }
        });
    }

    public void waitUntilReady() throws ExecutionException, InterruptedException {
        latestFuture.get();
    }

    public void progressUpdated(int progress, int max) {
        if (updateListener != null)
            updateListener.progressUpdated(progress, max);
    }

    public void throwError(String error) {
        if (errorListener != null)
            errorListener.error(error);
    }

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public interface ChangeListener {
        public void channelListChanged();
    }

    public interface InitFinishListener {
        public void initFinished();
    }

    public interface ProgressUpdateListener {
        public void progressUpdated(int progress, int max);
    }

    public interface ErrorListener {
        public void error(String error);
    }


    public void setChannel(Channel channel) {
        currentChannel = channel;
    }


}
