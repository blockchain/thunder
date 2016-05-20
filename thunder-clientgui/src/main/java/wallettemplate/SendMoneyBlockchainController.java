package wallettemplate;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import network.thunder.core.etc.Constants;
import org.bitcoinj.core.*;
import org.spongycastle.crypto.params.KeyParameter;
import wallettemplate.controls.BitcoinAddressValidator;
import wallettemplate.utils.TextFieldValidator;
import wallettemplate.utils.WTUtils;

import static com.google.common.base.Preconditions.checkState;
import static wallettemplate.utils.GuiUtils.checkGuiThread;
import static wallettemplate.utils.GuiUtils.informationalAlert;

public class SendMoneyBlockchainController {
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
    public void initialize () {
        Coin balance = Main.wallet.getBalance();
        checkState(!balance.isZero());
        new BitcoinAddressValidator(Constants.getNetwork(), address, sendBtn);
        new TextFieldValidator(amountEdit, text ->
                !WTUtils.didThrow(() -> checkState(Coin.parseCoin(text).compareTo(balance) <= 0)));
        amountEdit.setText(balance.toPlainString());
    }

    public void cancel (ActionEvent event) {
        overlayUI.done();
    }

    public void send (ActionEvent event) {
        // Address exception cannot happen as we validated it beforehand.
        try {
            Coin amount = Coin.parseCoin(amountEdit.getText());
            Address destination = new Address(Constants.getNetwork(), address.getText());
            Wallet.SendRequest req;
            if (amount.equals(Main.wallet.getBalance())) {
                req = Wallet.SendRequest.emptyWallet(destination);
            } else {
                req = Wallet.SendRequest.to(destination, amount);
            }
            req.ensureMinRequiredFee = true;
            req.feePerKb = Coin.valueOf(5000);
            req.aesKey = aesKey;
            sendResult = Main.wallet.sendCoins(req);

            overlayUI.done();
        } catch (InsufficientMoneyException e) {
            informationalAlert("Could not empty the wallet",
                    "You may have too little money left in the wallet to make a transaction.");
            overlayUI.done();
        } catch (ECKey.KeyIsEncryptedException e) {
            askForPasswordAndRetry();
        } catch (AddressFormatException e) {
            // Cannot happen because we already validated it when the text field changed.
            throw new RuntimeException(e);
        }
    }

    private void askForPasswordAndRetry () {
        Main.OverlayUI<WalletPasswordController> pwd = Main.instance.overlayUI("wallet_password.fxml");
        final String addressStr = address.getText();
        final String amountStr = amountEdit.getText();
        pwd.controller.aesKeyProperty().addListener((observable, old, cur) -> {
            // We only get here if the user found the right password. If they don't or they cancel, we end up back on
            // the main UI screen. By now the send money screen is history so we must recreate it.
            checkGuiThread();
            Main.OverlayUI<SendMoneyBlockchainController> screen = Main.instance.overlayUI("send_money_blockchain.fxml");
            screen.controller.aesKey = cur;
            screen.controller.address.setText(addressStr);
            screen.controller.amountEdit.setText(amountStr);
            screen.controller.send(null);
        });
    }

    private void updateTitleForBroadcast () {
        final int peers = sendResult.tx.getConfidence().numBroadcastPeers();
        titleLabel.setText(String.format("Broadcasting ... seen by %d peers", peers));
    }
}
