package wallettemplate.utils;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.helper.events.LNEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadProgressTracker;
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

    public BooleanProperty openChannelButtonEnabled = new SimpleBooleanProperty(true);
    public BooleanProperty sendReceiveButtonEnabled = new SimpleBooleanProperty(true);

    private SimpleObjectProperty<Address> address = new SimpleObjectProperty<>();
    public SimpleObjectProperty<Coin> balance = new SimpleObjectProperty<>(Coin.ZERO);
    public SimpleObjectProperty<Coin> balanceThunder = new SimpleObjectProperty<>(Coin.ZERO);
    public static SimpleDoubleProperty syncProgress = new SimpleDoubleProperty(-1);
    private ProgressBarUpdater syncProgressUpdater = new ProgressBarUpdater();

    public ObservableList<Node> transactions = FXCollections.observableArrayList();

    public BitcoinUIModel () {
    }

    public void init () {
        Main.thunderContext.addEventListener(new LNEventListener() {
            @Override
            public void onEvent () {
                new Thread(new Runnable() {
                    @Override
                    public void run () {
                        try {
                            Thread.sleep(300);
                            update();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    public void update () {
        Platform.runLater(() -> {
            openChannelButtonEnabled.setValue(Main.dbHandler.getOpenChannel().size() > 0 || Main.dbHandler.getIPObjects().size() == 0);
            sendReceiveButtonEnabled.setValue(Main.dbHandler.getOpenChannel().size() == 0);

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
