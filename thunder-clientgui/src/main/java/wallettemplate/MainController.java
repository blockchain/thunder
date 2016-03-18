package wallettemplate;

import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.messages.impl.results.NullResultCommand;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadProgressTracker;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.utils.MonetaryFormat;
import org.fxmisc.easybind.EasyBind;
import wallettemplate.controls.NotificationBarPane;
import wallettemplate.utils.BitcoinUIModel;

import java.util.ResourceBundle;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class MainController {
    public HBox controlsBox;

    @FXML
    private ResourceBundle resources;

    @FXML
    private TabPane tabPane;

    @FXML
    private Font x1;

    @FXML
    private Button receiveMoneyBtn;

    @FXML
    private Button sendMoneyOutBtn;

    @FXML
    private ListView blockchainTxList;

    @FXML
    private Label balance;

    @FXML
    private Color x2;

    @FXML
    private Label balance1;

    @FXML
    private Color x22;

    @FXML
    private Font x11;

    @FXML
    private Button openChannel;

    @FXML
    private Button thunderReceiveMoneyBtn;

    @FXML
    private Button thunderSendMoneyOutBtn;

    @FXML
    private Button thunderRefreshBtn;

    @FXML
    private Button startListenButton;

    @FXML
    private Button fetchIPButton;

    @FXML
    private Button syncButton;

    @FXML
    private ListView nodesList;

    @FXML
    private ListView channelNetworkList;

    @FXML
    private ListView channelList;

    @FXML
    private ListView thunderTxListIncluded;

    @FXML
    private ListView thunderTxListSettled;

    @FXML
    private ListView thunderTxListOpen;

    @FXML
    private ListView thunderTxListRefunded;

    @FXML
    private Label thunderBalance;

    @FXML
    private Color x21;

    @FXML
    private Color x211;

    @FXML
    void fetchIPs (ActionEvent event) {
        Main.thunderContext.fetchNetworkIPs(new NullResultCommand());
    }

    @FXML
    void startListen (ActionEvent event) {
        Main.thunderContext.startListening(new NullResultCommand());
    }

    @FXML
    void syncThunder (ActionEvent event) {
        if (selectedNode != null) {
            Main.thunderContext.openChannel(selectedNode.pubkey, new NullResultCommand());
        }
    }

    @FXML
    void onReloadButton (ActionEvent event) {
        Main.thunderContext.getSyncData(result -> {
            System.out.println("Sync finished: " + result.getMessage());
        });
    }

    @FXML
    void refresh (ActionEvent event) {
        model.update();
    }

    private PubkeyIPObject selectedNode = null;

    private BitcoinUIModel model = new BitcoinUIModel();
    private NotificationBarPane.Item syncItem;

    // Called by FXMLLoader.
    public void initialize () {

    }

    public void onBitcoinSetup () {

        model.init();
        balance.textProperty().bind(EasyBind.map(model.balanceProperty(), Coin::toFriendlyString));
        // Don't let the user click send money when the wallet is empty.
        sendMoneyOutBtn.disableProperty().bind(model.balanceProperty().isEqualTo(Coin.ZERO));

        Bindings.bindContent(blockchainTxList.getItems(), model.transactions);
        Bindings.bindContent(nodesList.getItems(), model.ipList);
        Bindings.bindContent(channelNetworkList.getItems(), model.channelNetworkList);
        Bindings.bindContent(channelList.getItems(), model.channelList);

        thunderBalance.textProperty().bind(EasyBind.map(model.balanceThunder, Coin::toFriendlyString));

        nodesList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<PubkeyIPObject>() {
            @Override
            public void changed (ObservableValue<? extends PubkeyIPObject> observable, PubkeyIPObject oldValue, PubkeyIPObject newValue) {
                System.out.println("ListView selection changed from oldValue = "
                        + oldValue + " to newValue = " + newValue);
                selectedNode = newValue;
            }
        });

        blockchainTxList.setCellFactory(param1 -> new TextFieldListCell(new StringConverter<Transaction>() {
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
        blockchainTxList.setCellFactory(param -> new TextFieldListCell(new StringConverter<Transaction>() {
            @Override
            public String toString (Transaction tx) {
                Coin value = tx.getValue(Main.bitcoin.wallet());
                if (value.isPositive()) {
                    return tx.getConfidence().getDepthInBlocks() + " Incoming payment of " + MonetaryFormat.BTC.format(value);
                } else if (value.isNegative()) {
                    Address address = tx.getOutput(0).getAddressFromP2PKHScript(Main.params);
                    if (address == null) {
                        return tx.getConfidence().getDepthInBlocks() + " Outbound payment to ThunderChannel of " + value.toFriendlyString().substring
                                (1);
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

    }

    private void showBitcoinSyncMessage () {
    }

    public void sendMoneyOut (ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        Main.instance.overlayUI("send_money_blockchain.fxml");
    }

    public void settingsClicked (ActionEvent event) {
        Main.OverlayUI<WalletSettingsController> screen = Main.instance.overlayUI("wallet_settings.fxml");
        screen.controller.initialize(null);
    }

    public void restoreFromSeedAnimation () {
        // Buttons slide out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(1200), controlsBox);
        leave.setByY(80.0);
        leave.play();
    }

    public void readyToGoAnimation () {
        // Buttons slide in and clickable address appears simultaneously.
    }

    public DownloadProgressTracker progressBarUpdater () {
        return model.getDownloadProgressTracker();
    }

    public void receiveMoney (ActionEvent actionEvent) {
        Main.instance.overlayUI("receive_money_blockchain.fxml");
    }

    @FXML
    void thunderReceiveMoney (ActionEvent event) {
        Main.instance.overlayUI("receive_money_request.fxml");
    }

    @FXML
    void thunderSendMoneyOut (ActionEvent event) {
        Main.instance.overlayUI("send_money.fxml");
    }

    @FXML
    void openChannel (ActionEvent event) {
        if (selectedNode != null) {
            Main.thunderContext.openChannel(selectedNode.pubkey, new NullResultCommand());
        }
    }

    public void closeChannel (ActionEvent actionEvent) {
        //TODO
    }

    @FXML
    void thunderRefresh (ActionEvent event) {
    }
}
