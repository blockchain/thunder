package wallettemplate;

import network.thunder.client.api.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.bitcoinj.core.*;
import org.spongycastle.crypto.params.KeyParameter;
import wallettemplate.controls.BitcoinAddressValidator;
import wallettemplate.controls.ThunderAddressValidator;
import wallettemplate.utils.BitcoinUIModel;
import wallettemplate.utils.TextFieldValidator;
import wallettemplate.utils.WTUtils;

import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkState;
import static wallettemplate.utils.GuiUtils.*;

public class SendMoneyController {
    public Button sendBtn;
    public Button cancelBtn;
    public TextField address;
    public Label titleLabel;
    public TextField amountEdit;
    public Label btcLabel;

    public Main.OverlayUI overlayUI;

    private Wallet.SendResult sendResult;
    private KeyParameter aesKey;

    // Called by FXMLLoader
    public void initialize() throws SQLException {

        Coin balance = ThunderContext.getAmountClientAccessible();
//        Coin balance = Coin.valueOf(1000);
        checkState(!balance.isZero());
        new ThunderAddressValidator(Main.params, address, sendBtn);
        new TextFieldValidator(amountEdit, text ->
                !WTUtils.didThrow(() -> checkState(Coin.parseCoin(text).compareTo(balance) <= 0)));
        amountEdit.setText(Coin.valueOf(1000).toPlainString());
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void send(ActionEvent event) {
        // Address exception cannot happen as we validated it beforehand.
        Coin amount = Coin.parseCoin(amountEdit.getText());

        try {
            Main.instance.notificationBar.pushItem("Sending Payment..", BitcoinUIModel.syncProgress);

            ThunderContext.makePayment(amount.getValue(), address.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        overlayUI.done();

    }

    private void askForPasswordAndRetry() {
        Main.OverlayUI<WalletPasswordController> pwd = Main.instance.overlayUI("wallet_password.fxml");
        final String addressStr = address.getText();
        final String amountStr = amountEdit.getText();
        pwd.controller.aesKeyProperty().addListener((observable, old, cur) -> {
            // We only get here if the user found the right password. If they don't or they cancel, we end up back on
            // the main UI screen. By now the send money screen is history so we must recreate it.
            checkGuiThread();
            Main.OverlayUI<SendMoneyController> screen = Main.instance.overlayUI("send_money_blockchain.fxml");
            screen.controller.aesKey = cur;
            screen.controller.address.setText(addressStr);
            screen.controller.amountEdit.setText(amountStr);
            screen.controller.send(null);
        });
    }

    private void updateTitleForBroadcast() {
        final int peers = sendResult.tx.getConfidence().numBroadcastPeers();
        titleLabel.setText(String.format("Broadcasting ... seen by %d peers", peers));
    }
}
