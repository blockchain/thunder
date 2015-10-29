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
package network.thunder.server.api;

import network.thunder.server.database.MySQLConnection;
import network.thunder.server.database.objects.Channel;
import network.thunder.server.database.objects.Output;
import network.thunder.server.database.objects.Payment;
import network.thunder.server.etc.Tools;
import network.thunder.server.wallet.TransactionStorage;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Wallet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

// TODO: Auto-generated Javadoc

/**
 * The Class ThunderContext.
 */
public class ThunderContext {

    /**
     * The conn.
     */
    public static Connection conn;
    /**
     * The output list.
     */
    public static ArrayList<Output> outputList = new ArrayList<Output>();
    /**
     * The channel list.
     */
    public static ArrayList<Channel> channelList = new ArrayList<Channel>();
    /**
     * The current channel.
     */
    public static Channel currentChannel;
    /**
     * The transaction storage.
     */
    public static TransactionStorage transactionStorage;
    /**
     * The payment list included.
     */
    private static ArrayList<Payment> paymentListIncluded = new ArrayList<Payment>();
    /**
     * The payment list settled.
     */
    private static ArrayList<Payment> paymentListSettled = new ArrayList<Payment>();
    /**
     * The payment list refunded.
     */
    private static ArrayList<Payment> paymentListRefunded = new ArrayList<Payment>();
    /**
     * The payment list open.
     */
    private static ArrayList<Payment> paymentListOpen = new ArrayList<Payment>();
    /**
     * The wallet.
     */
    private static Wallet wallet;
    /**
     * The peer group.
     */
    private static PeerGroup peerGroup;
    /**
     * The listeners.
     */
    private static ArrayList<ChangeListener> listeners = new ArrayList<ChangeListener>();

    /**
     * The init listener.
     */
    private static InitFinishListener initListener;

    /**
     * The update listener.
     */
    private static ProgressUpdateListener updateListener;

    /**
     * The error listener.
     */
    private static ErrorListener errorListener;

    /**
     * The first.
     */
    private static boolean first = true;

    /**
     * Adds the listener.
     *
     * @param toAdd the to add
     */
    public static void addListener (ChangeListener toAdd) {
        System.out.println("Listener added!");
        listeners.add(toAdd);
    }

    /**
     * Close channel.
     *
     * @throws Exception the exception
     */
    public static void closeChannel () throws Exception {

        new Thread(new Runnable() {
            public void run () {
                try {

                    //					ClientTools.closeChannel(conn, currentChannel, peerGroup);
                    channelList.remove(currentChannel);
                    currentChannel = null;

                    for (ChangeListener listener : listeners) {
                        listener.channelListChanged();
                    }

                } catch (Exception e) {
                    throwError(Tools.stacktraceToString(e));
                    e.printStackTrace();
                }

            }
        }).start();

    }

    /**
     * Gets the amount client.
     *
     * @return the amount client
     */
    public static Coin getAmountClient () {
        if (currentChannel == null) {
            return Coin.ZERO;
        }
        return Coin.valueOf(currentChannel.getAmountClient());
    }

    /**
     * Gets the amount client accessible.
     *
     * @return the amount client accessible
     * @throws SQLException the SQL exception
     */
    public static Coin getAmountClientAccessible () throws SQLException {
        if (currentChannel == null) {
            return Coin.ZERO;
        }
        if (currentChannel.getChannelTxClientID() == 0) {
            return Coin.valueOf(currentChannel.getAmountClient());
        } else {
            return currentChannel.getChannelTxClient().getOutput(0).getValue();
        }
    }

    /**
     * Gets the channel list.
     *
     * @return the channel list
     */
    public static ArrayList<Channel> getChannelList () {
        return channelList;
    }

    /**
     * Gets the current channel.
     *
     * @return the current channel
     */
    public static Channel getCurrentChannel () {
        return currentChannel;
    }

    /**
     * Gets the payment list included.
     *
     * @return the payment list included
     */
    public static ArrayList<Payment> getPaymentListIncluded () {
        return paymentListIncluded;
    }

    /**
     * Gets the payment list open.
     *
     * @return the payment list open
     */
    public static ArrayList<Payment> getPaymentListOpen () {
        return paymentListOpen;
    }

    /**
     * Gets the payment list refunded.
     *
     * @return the payment list refunded
     */
    public static ArrayList<Payment> getPaymentListRefunded () {
        return paymentListRefunded;
    }

    /**
     * Gets the payment list settled.
     *
     * @return the payment list settled
     */
    public static ArrayList<Payment> getPaymentListSettled () {
        return paymentListSettled;
    }

    /**
     * Gets the payment receive request.
     *
     * @param amount the amount
     * @return the payment receive request
     * @throws Exception the exception
     */
    public static PaymentRequest getPaymentReceiveRequest (long amount) throws Exception {

        Payment p = new Payment(0, currentChannel.getId(), amount);
        p.setReceiver(currentChannel.getPubKeyClient());
        p.paymentToServer = false;

        MySQLConnection.addPayment(conn, p);
        PaymentRequest request = new PaymentRequest(currentChannel, p);

        updatePaymentLists();
        for (ChangeListener listener : listeners) {
            listener.channelListChanged();
        }

        return request;

    }

    /**
     * Checks for active channel.
     *
     * @return true, if successful
     */
    public static boolean hasActiveChannel () {
        return (channelList.size() != 0);
    }

    /**
     * Inits the.
     *
     * @param w        the w
     * @param p        the p
     * @param clientId the client id
     * @throws SQLException           the SQL exception
     * @throws IOException            Signals that an I/O exception has occurred.
     * @throws InstantiationException the instantiation exception
     * @throws IllegalAccessException the illegal access exception
     * @throws ClassNotFoundException the class not found exception
     */
    public static void init (Wallet w, PeerGroup p, int clientId) throws SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (first) {
            System.out.println("Start init!");
            conn = MySQLConnection.getInstance(clientId);
            MySQLConnection.buildDatabase(conn);
            wallet = w;
            peerGroup = p;

            channelList = MySQLConnection.getActiveChannels(conn);
            if (channelList.size() > 0) {
                currentChannel = channelList.get(0);
                updatePaymentLists();
            }

            //			transactionStorage = TransactionStorage.initialize(conn);
            //			wallet.addEventListener(transactionStorage);
            System.out.println("Finished init! Active channels: " + channelList.size());

            //			transactionStorage.updateOutputs(wallet);
            if (initListener != null) {
                initListener.initFinished();
            }
            first = false;
        }
    }

    /**
     * Inits the.
     *
     * @param w the w
     * @param p the p
     * @throws SQLException           the SQL exception
     * @throws IOException            Signals that an I/O exception has occurred.
     * @throws InstantiationException the instantiation exception
     * @throws IllegalAccessException the illegal access exception
     * @throws ClassNotFoundException the class not found exception
     */
    public static void init (Wallet w, PeerGroup p) throws SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        init(w, p, 1);
    }

    /**
     * Make payment.
     *
     * @param amount  the amount
     * @param address the address
     * @throws Exception the exception
     */
    public static void makePayment (final long amount, final String address) throws Exception {

        new Thread(new Runnable() {
            public void run () {
                try {

                    PaymentRequest request = new PaymentRequest(currentChannel, amount, address);

                    //					currentChannel = ClientTools.makePayment(conn, currentChannel, request.getPayment());
                    updatePaymentLists();
                    for (ChangeListener listener : listeners) {
                        listener.channelListChanged();
                    }

                } catch (Exception e) {
                    throwError(Tools.stacktraceToString(e));
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Open channel.
     *
     * @param clientAmount the client amount
     * @param serverAmount the server amount
     * @param timeInDays   the time in days
     * @throws Exception the exception
     */
    public static void openChannel (final long clientAmount, final long serverAmount, final int timeInDays) throws Exception {
        new Thread(new Runnable() {
            public void run () {

                try {
                    System.out.println("New Thread..");
                    Channel channel = currentChannel;

                    //					channel = ClientTools.createChannel(conn, wallet, peerGroup, clientAmount, serverAmount, timeInDays);

                    channelList.add(channel);
                    currentChannel = channel;

                    for (ChangeListener listener : listeners) {
                        listener.channelListChanged();
                    }
                } catch (Exception e) {
                    throwError(Tools.stacktraceToString(e));
                    e.printStackTrace();
                }

            }
        }).start();

    }

    /**
     * Progress updated.
     *
     * @param progress the progress
     * @param max      the max
     */
    public static void progressUpdated (int progress, int max) {
        if (updateListener != null) {
            updateListener.progressUpdated(progress, max);
        }
    }

    /**
     * Sets the channel.
     *
     * @param channel the new channel
     */
    public static void setChannel (Channel channel) {
        currentChannel = channel;
    }

    /**
     * Sets the error listener.
     *
     * @param listener the new error listener
     */
    public static void setErrorListener (ErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Sets the inits the finished listener.
     *
     * @param listener the new inits the finished listener
     */
    public static void setInitFinishedListener (InitFinishListener listener) {
        initListener = listener;
    }

    /**
     * Sets the progress update listener.
     *
     * @param listener the new progress update listener
     */
    public static void setProgressUpdateListener (ProgressUpdateListener listener) {
        updateListener = listener;
    }

    /**
     * Throw error.
     *
     * @param error the error
     */
    public static void throwError (String error) {
        if (errorListener != null) {
            errorListener.error(error);
        }
    }

    /**
     * Update channel.
     *
     * @throws Exception the exception
     */
    public static void updateChannel () throws Exception {
        new Thread(new Runnable() {
            public void run () {
                try {
                    /**
                     * TODO: change protocol, such that server sends amount of new payments
                     * 			with the first response, such that we know whether we should update
                     * 			at all.
                     */
                    //					currentChannel = ClientTools.updateChannel(conn, currentChannel, true);

                    updatePaymentLists();
                    for (ChangeListener listener : listeners) {
                        listener.channelListChanged();
                    }

                    System.out.println("First finished!");

                    //					currentChannel = ClientTools.updateChannel(conn, currentChannel, false);

                    updatePaymentLists();
                    for (ChangeListener listener : listeners) {
                        listener.channelListChanged();
                    }

                    ThunderContext.progressUpdated(10, 10);

                } catch (Exception e) {
                    throwError(Tools.stacktraceToString(e));
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Update payment lists.
     *
     * @throws SQLException the SQL exception
     */
    private static void updatePaymentLists () throws SQLException {
        paymentListIncluded = MySQLConnection.getPaymentsIncludedInChannel(conn, currentChannel.getId());
        paymentListSettled = MySQLConnection.getPaymentsSettled(conn, currentChannel.getId());
        paymentListRefunded = MySQLConnection.getPaymentsRefunded(conn, currentChannel.getId());
        paymentListOpen = MySQLConnection.getPaymentsOpen(conn, currentChannel.getId());
    }

    /**
     * The listener interface for receiving change events.
     * The class that is interested in processing a change
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addChangeListener<code> method. When
     * the change event occurs, that object's appropriate
     * method is invoked.
     *
     * @see ChangeEvent
     */
    public interface ChangeListener {

        /**
         * Channel list changed.
         */
        public void channelListChanged ();
    }

    /**
     * The listener interface for receiving initFinish events.
     * The class that is interested in processing a initFinish
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addInitFinishListener<code> method. When
     * the initFinish event occurs, that object's appropriate
     * method is invoked.
     *
     * @see InitFinishEvent
     */
    public interface InitFinishListener {

        /**
         * Inits the finished.
         */
        public void initFinished ();
    }

    /**
     * The listener interface for receiving progressUpdate events.
     * The class that is interested in processing a progressUpdate
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addProgressUpdateListener<code> method. When
     * the progressUpdate event occurs, that object's appropriate
     * method is invoked.
     *
     * @see ProgressUpdateEvent
     */
    public interface ProgressUpdateListener {

        /**
         * Invoked when progress update occurs.
         *
         * @param progress the progress
         * @param max      the max
         */
        public void progressUpdated (int progress, int max);
    }

    /**
     * The listener interface for receiving error events.
     * The class that is interested in processing a error
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addErrorListener<code> method. When
     * the error event occurs, that object's appropriate
     * method is invoked.
     *
     * @see ErrorEvent
     */
    public interface ErrorListener {

        /**
         * Error.
         *
         * @param error the error
         */
        public void error (String error);
    }

}
