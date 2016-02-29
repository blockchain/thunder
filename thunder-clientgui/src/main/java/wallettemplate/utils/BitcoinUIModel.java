package wallettemplate.utils;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.ChannelStatusObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventListener;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.PaymentWrapper;
import org.bitcoinj.core.*;
import wallettemplate.Main;

import java.util.Date;

/**
 * A class that exposes relevant bitcoin stuff as JavaFX bindable properties.
 */
public class BitcoinUIModel {

    public ObservableList<PubkeyIPObject> ipList = FXCollections.observableArrayList();
    public ObservableList<Node> channelNetworkList = FXCollections.observableArrayList();
    public ObservableList<Channel> channelList = FXCollections.observableArrayList();

    public ObservableList<Node> transactionsThunderIncluded = FXCollections.observableArrayList();
    public ObservableList<Node> transactionsThunderSettled = FXCollections.observableArrayList();
    public ObservableList<Node> transactionsThunderRefunded = FXCollections.observableArrayList();
    public ObservableList<Node> transactionsThunderOpen = FXCollections.observableArrayList();

    private SimpleObjectProperty<Address> address = new SimpleObjectProperty<>();
    public SimpleObjectProperty<Coin> balance = new SimpleObjectProperty<>(Coin.ZERO);
    public SimpleObjectProperty<Coin> balanceThunder = new SimpleObjectProperty<>(Coin.ZERO);
    public static SimpleDoubleProperty syncProgress = new SimpleDoubleProperty(-1);
    private ProgressBarUpdater syncProgressUpdater = new ProgressBarUpdater();

    public ObservableList<Node> transactions = FXCollections.observableArrayList();

    public BitcoinUIModel () {
    }

    public void init () {
        Main.bitcoin.wallet().addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onWalletChanged (Wallet wallet) {
                super.onWalletChanged(wallet);
                update();
            }
        }, Platform::runLater);

        Main.thunderContext.addEventListener(new LNEventListener() {
            @Override
            public void onEvent () {
                new Thread(new Runnable() {
                    @Override
                    public void run () {
                        try {
                            Thread.sleep(100);
                            update();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        update();
    }

    public void update () {
        Platform.runLater(() -> {
            balance.set(Main.bitcoin.wallet().getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE));
            address.set(Main.bitcoin.wallet().currentReceiveAddress());

            ObservableList<Node> items0 = FXCollections.observableArrayList();
            for (Transaction p : Main.bitcoin.wallet().getTransactionsByTime()) {
                Label label = new Label(p.toString());
                items0.add(label);
            }
            transactionsThunderIncluded.setAll(items0);

            ObservableList<Node> items1 = FXCollections.observableArrayList();
            for (PaymentWrapper p : Main.dbHandler.getAllPayments()) {
                Label label = new Label(p.toString());
                items1.add(label);
            }
            transactionsThunderIncluded.setAll(items1);

            ObservableList<Node> items2 = FXCollections.observableArrayList();
            for (PaymentWrapper p : Main.dbHandler.getRedeemedPayments()) {
                Label label = new Label(p.toString());
                items2.add(label);
            }
            transactionsThunderSettled.setAll(items2);

            ObservableList<Node> items3 = FXCollections.observableArrayList();
            for (PaymentWrapper p : Main.dbHandler.getRefundedPayments()) {
                Label label = new Label(p.toString());
                items3.add(label);
            }
            transactionsThunderRefunded.setAll(items3);

            ObservableList<Node> items4 = FXCollections.observableArrayList();
            for (PaymentWrapper p : Main.dbHandler.getOpenPayments()) {
                Label label = new Label(p.toString());
                items4.add(label);
            }
            transactionsThunderOpen.setAll(items4);

            ObservableList<PubkeyIPObject> items5 = FXCollections.observableArrayList();
            for (PubkeyIPObject p : Main.dbHandler.getIPObjects()) {
                items5.add(p);
            }
            ipList.setAll(items5);

            ObservableList<Node> items6 = FXCollections.observableArrayList();
            for (ChannelStatusObject p : Main.dbHandler.getTopology()) {
                Label label = new Label(p.toString());
                items6.add(label);
            }
            channelNetworkList.setAll(items6);

            long totalAmount = 0;
            ObservableList<Channel> items7 = FXCollections.observableArrayList();
            for (Channel p : Main.dbHandler.getOpenChannel()) {
                totalAmount += p.channelStatus.amountServer;
                items7.add(p);
            }

            balanceThunder.set(Coin.valueOf(totalAmount));

            channelList.setAll(items7);
        });
    }

    private class ProgressBarUpdater extends DownloadProgressTracker {
        @Override
        protected void progress (double pct, int blocksLeft, Date date) {
            super.progress(pct, blocksLeft, date);
            Platform.runLater(() -> syncProgress.set(pct / 100.0));
        }

        @Override
        protected void doneDownload () {
            super.doneDownload();

        }
    }

    public DownloadProgressTracker getDownloadProgressTracker () {
        return syncProgressUpdater;
    }

    public ReadOnlyDoubleProperty syncProgressProperty () {
        return syncProgress;
    }

    public ReadOnlyObjectProperty<Address> addressProperty () {
        return address;
    }

    public ReadOnlyObjectProperty<Coin> balanceProperty () {
        return balance;
    }

}
