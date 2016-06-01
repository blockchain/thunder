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
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.SyncListener;
import network.thunder.core.helper.callback.results.NullResultCommand;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.utils.MonetaryFormat;
import org.fxmisc.easybind.EasyBind;
import wallettemplate.controls.NotificationBarPane;
import wallettemplate.utils.BitcoinUIModel;
import wallettemplate.utils.GuiUtils;

import java.util.List;
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
    private Label blockchainBalance;

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
        Main.thunderContext.getSyncData(new SyncListener() {
            @Override
            public void onSegmentSynced (int segment) {
                System.out.println("Segment synced: " + segment);
            }

            @Override
            public void onSyncFinished () {
                System.out.println("MainController.onSyncFinished");
            }

            @Override
            public void onSyncFailed () {
                System.out.println("MainController.onSyncFailed");
            }
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
        Bindings.bindContent(thunderTxListIncluded.getItems(), model.transactionsThunderIncluded);
        Bindings.bindContent(thunderTxListOpen.getItems(), model.transactionsThunderOpen);
        Bindings.bindContent(thunderTxListRefunded.getItems(), model.transactionsThunderRefunded);
        Bindings.bindContent(thunderTxListSettled.getItems(), model.transactionsThunderSettled);

        thunderBalance.textProperty().bind(EasyBind.map(model.balanceThunder, Coin::toFriendlyString));
        blockchainBalance.textProperty().bind(EasyBind.map(model.balance, Coin::toFriendlyString));

        openChannel.textProperty().bind(model.openChannelButtonText);

        thunderReceiveMoneyBtn.disableProperty().bind(model.sendReceiveButtonEnabled);
        thunderSendMoneyOutBtn.disableProperty().bind(model.sendReceiveButtonEnabled);

        nodesList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<PubkeyIPObject>() {
            @Override
            public void changed (ObservableValue<? extends PubkeyIPObject> observable, PubkeyIPObject oldValue, PubkeyIPObject newValue) {
                if (newValue != null) {
                    selectedNode = newValue;
                }
            }
        });

        nodesList.setCellFactory(param1 -> new TextFieldListCell(new StringConverter<PubkeyIPObject>() {
            @Override
            public String toString (PubkeyIPObject tx) {
                return tx.hostname;
            }

            @Override
            public PubkeyIPObject fromString (String string) {
                return null;
            }
        }));

        thunderTxListIncluded.setCellFactory(param1 -> new TextFieldListCell(new StringConverter<PaymentWrapper>() {
            @Override
            public String toString (PaymentWrapper channel) {
                String text = "";
                String status = "";
                if (channel.paymentData.sending) {
                    text += "[ -> ]";
                    status += channel.statusSender;
                } else {
                    text += "[ <- ]";
                    status += channel.statusReceiver;
                }

                text += " " + Coin.valueOf(channel.paymentData.amount).toFriendlyString() + " ";

                text += "<" + Tools.bytesToHex(channel.paymentData.secret.hash).substring(0, 8) + "..." + ">";

                text += " [" + status + "]";

                return text;
            }

            @Override
            public PaymentWrapper fromString (String string) {
                return null;
            }
        }));

        channelList.setCellFactory(param1 -> new TextFieldListCell(new StringConverter<Channel>() {
            @Override
            public String toString (Channel channel) {
                return Coin.valueOf(channel.channelStatus.amountServer).toFriendlyString() +
                        " with " + model.getHostname(Main.dbHandler.getIPObjects(), channel.nodeKeyClient.getPubKey());
            }

            @Override
            public Channel fromString (String string) {
                return null;
            }
        }));

        blockchainTxList.setCellFactory(param1 -> new TextFieldListCell(new StringConverter<Transaction>() {
            @Override
            public String toString (Transaction tx) {
                Coin value = tx.getValue(Main.wallet);
                if (value.isPositive()) {
                    return tx.getConfidence().getDepthInBlocks() + " Incoming payment of " + MonetaryFormat.BTC.format(value);
                } else if (value.isNegative()) {
                    Address address = tx.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork());
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
                Coin value = tx.getValue(Main.wallet);
                if (value.isPositive()) {
                    return tx.getConfidence().getDepthInBlocks() + " Incoming payment of " + MonetaryFormat.BTC.format(value);
                } else if (value.isNegative()) {
                    Address address = tx.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork());
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
            List<Channel> openChannel = Main.dbHandler.getOpenChannel();
            if (openChannel.size() == 0) {
                Main.thunderContext.openChannel(selectedNode.pubkey, new NullResultCommand());
            } else {
                Main.thunderContext.closeChannel(openChannel.get(0), new NullResultCommand());
            }
        } else {
            GuiUtils.informationalAlert("ThunderNetwork Wallet", "No node to open channel with selected..");
        }
    }

    @FXML
    void thunderRefresh (ActionEvent event) {
    }
}
