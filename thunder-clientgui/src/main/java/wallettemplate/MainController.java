package wallettemplate;

import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.util.StringConverter;
import network.thunder.client.api.ThunderContext;
import org.bitcoinj.core.*;
import org.bitcoinj.utils.MonetaryFormat;
import org.fxmisc.easybind.EasyBind;
import wallettemplate.controls.NotificationBarPane;
import wallettemplate.utils.BitcoinUIModel;

import java.sql.SQLException;

import static wallettemplate.Main.bitcoin;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class MainController {
    public HBox controlsBox;
    @FXML
    private TabPane tabPane;

    @FXML
    private Font x1;

    @FXML
    private Label balance;
    @FXML
    private Label balance1;

    @FXML
    private Color x2;

    @FXML
    private Font x3;

    @FXML
    private Color x4;

    @FXML
    private Button receiveMoneyBtn;

    @FXML
    private Button sendMoneyOutBtn;

    @FXML
    private ListView<Transaction> blockchainTxList;

    @FXML
    private Font x11;

    @FXML
    private Label thunderBalance;
    @FXML
    private Label thunderBalance1;

    @FXML
    private Color x21;

    @FXML
    private Font x31;

    @FXML
    private Color x41;

    @FXML
    private ListView<Node> thunderTxListIncluded;

    @FXML
    private ListView<Node> thunderTxListSettled;

    @FXML
    private ListView<Node> thunderTxListRefunded;

    @FXML
    private ListView<Node> thunderTxListOpen;

    @FXML
    private Button openChannel;

    @FXML
    private Button thunderRefreshBtn;

    @FXML
    private Button thunderReceiveMoneyBtn;

    @FXML
    private Button thunderSendMoneyOutBtn;

    private BitcoinUIModel model = new BitcoinUIModel();
    private NotificationBarPane.Item syncItem;

    public void closeChannel (ActionEvent actionEvent) {

    }

    // Called by FXMLLoader.
    public void initialize () {
        //        addressControl.setOpacity(0.0);
    }

    public void onBitcoinSetup () {

        model.setWallet(bitcoin.wallet());
        //        addressControl.addressProperty().bind(model.addressProperty());
        balance.textProperty().bind(EasyBind.map(model.balanceProperty(), Coin::toFriendlyString));
        // Don't let the user click send money when the wallet is empty.
        sendMoneyOutBtn.disableProperty().bind(model.balanceProperty().isEqualTo(Coin.ZERO));

        TorClient torClient = Main.bitcoin.peerGroup().getTorClient();
        if (torClient != null) {
            SimpleDoubleProperty torProgress = new SimpleDoubleProperty(-1);
            String torMsg = "Initialising Tor";
            syncItem = Main.instance.notificationBar.pushItem(torMsg, torProgress);
            torClient.addInitializationListener(new TorInitializationListener() {
                @Override
                public void initializationProgress (String message, int percent) {
                    Platform.runLater(() -> {
                        syncItem.label.set(torMsg + ": " + message);
                        torProgress.set(percent / 100.0);
                    });
                }

                @Override
                public void initializationCompleted () {
                    Platform.runLater(() -> {
                        syncItem.cancel();
                        showBitcoinSyncMessage();

                    });
                }
            });
        } else {

            showBitcoinSyncMessage();
        }
        model.syncProgressProperty().addListener(x -> {
            if (model.syncProgressProperty().get() >= 1.0) {
                readyToGoAnimation();

                if (syncItem != null) {
                    syncItem.cancel();
                    syncItem = null;
                }
            } else if (syncItem == null) {
                showBitcoinSyncMessage();
            }
        });
        Bindings.bindContent(blockchainTxList.getItems(), model.getTransactions());

        thunderTxListIncluded.setItems(model.getTransactionsThunderIncluded());
        thunderTxListSettled.setItems(model.getTransactionsThunderSettled());
        thunderTxListRefunded.setItems(model.getTransactionsThunderRefunded());
        thunderTxListOpen.setItems(model.getTransactionsThunderOpen());

        blockchainTxList.setCellFactory(param -> new TextFieldListCell<>(new StringConverter<Transaction>() {
            @Override
            public String toString (Transaction tx) {
                Coin value = tx.getValue(Main.bitcoin.wallet());
                if (value.isPositive()) {
                    return tx.getConfidence().getDepthInBlocks() + " Incoming payment of " + MonetaryFormat.BTC.format(value);
                } else if (value.isNegative()) {
                    Address address = tx.getOutput(0).getAddressFromP2PKHScript(Main.params);
                    if (address == null) {
                        return tx.getConfidence().getDepthInBlocks() + " Outbound payment to ThunderChannel of " + value.toFriendlyString().substring(1);
                    }
                    return tx.getConfidence().getDepthInBlocks() + " Outbound payment to " + address;
                }
                return "Payment with id " + tx.getHash();
            }

            @Override
            public Transaction fromString (String string) {
                return null;
            }
        }));

        ThunderContext.instance.addListener(new ThunderContext.ChangeListener() {
            @Override
            public void channelListChanged () {
                update();
            }
        });

        ThunderContext.instance.setInitFinishedListener(new ThunderContext.InitFinishListener() {
            @Override
            public void initFinished () {
                update();
            }
        });

        ThunderContext.instance.setUpdateStartListener(() -> {
            Platform.runLater(() -> Main.instance.notificationBar.pushItem("Updating Channel..", BitcoinUIModel.syncProgress));
        });

    }

    @FXML
    void openChannel (ActionEvent event) {
        if (ThunderContext.instance.hasActiveChannel()) {
            Main.instance.overlayUI("channel_info.fxml");
        } else {
            //            syncItem.label.set("Open Channel..");

            Main.instance.overlayUI("create_channel.fxml");
        }
    }

    public DownloadProgressTracker progressBarUpdater () {
        return model.getDownloadProgressTracker();
    }

    public void readyToGoAnimation () {
        // Buttons slide in and clickable address appears simultaneously.
        //        TranslateTransition arrive = new TranslateTransition(Duration.millis(1200), controlsBox);
        //        arrive.setInterpolator(new ElasticInterpolator(EasingMode.EASE_OUT, 1, 2));
        //        arrive.setToY(0.0);
        //        FadeTransition reveal = new FadeTransition(Duration.millis(1200), addressControl);
        //        reveal.setToValue(1.0);
        //        ParallelTransition group = new ParallelTransition(arrive, reveal);
        //        group.setDelay(NotificationBarPane.ANIM_OUT_DURATION);
        //        group.setCycleCount(1);
        //        group.play();
    }

    public void receiveMoney (ActionEvent actionEvent) {

        Main.instance.overlayUI("receive_money_blockchain.fxml");
    }

    public void restoreFromSeedAnimation () {
        // Buttons slide out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(1200), controlsBox);
        leave.setByY(80.0);
        leave.play();
    }

    public void sendMoneyOut (ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        Main.instance.overlayUI("send_money_blockchain.fxml");
    }

    public void settingsClicked (ActionEvent event) {
        Main.OverlayUI<WalletSettingsController> screen = Main.instance.overlayUI("wallet_settings.fxml");
        screen.controller.initialize(null);
    }

    private void showBitcoinSyncMessage () {
        syncItem = Main.instance.notificationBar.pushItem("Synchronising with the Bitcoin network", model.syncProgressProperty());
    }

    @FXML
    void thunderReceiveMoney (ActionEvent event) {
        Main.instance.overlayUI("receive_money.fxml");
    }

    @FXML
    void thunderRefresh (ActionEvent event) {
        try {
            Main.instance.notificationBar.pushItem("Updating Channel..", BitcoinUIModel.syncProgress);
            ThunderContext.instance.updateChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void thunderSendMoneyOut (ActionEvent event) {

        Main.instance.overlayUI("send_money.fxml");
    }

    private void update () {

        Platform.runLater(() -> {

            boolean activeChannel = ThunderContext.instance.hasActiveChannel();
            thunderReceiveMoneyBtn.setDisable(!activeChannel);
            thunderSendMoneyOutBtn.setDisable(!activeChannel);
            thunderRefreshBtn.setDisable(!activeChannel);

            if (activeChannel) {
                openChannel.setText("Channel Info");
            } else {
                openChannel.setText("Open Channel");
            }

            balance1.setText("(" + bitcoin.wallet().getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString() + ")");

            try {
                Coin c1 = ThunderContext.instance.getAmountClient();
                Coin c2 = ThunderContext.instance.getAmountClientAccessible();
                if (c1.equals(c2)) {
                    thunderBalance1.setVisible(false);
                } else {
                    thunderBalance1.setVisible(true);
                }
                thunderBalance.setText(c1.toFriendlyString());
                thunderBalance1.setText(c2.toFriendlyString());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            System.out.println("LISTENER CALLED!!!");
        });
    }
}
