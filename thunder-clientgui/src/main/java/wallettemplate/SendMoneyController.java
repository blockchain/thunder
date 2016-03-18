package wallettemplate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.Result;
import network.thunder.core.communication.objects.messages.interfaces.helper.etc.ResultCommand;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Wallet;
import org.spongycastle.crypto.params.KeyParameter;
import wallettemplate.utils.GuiUtils;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.ByteBuffer;

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

    long amount;
    byte[] hash = new byte[20];
    byte[] destination = new byte[33];

    // Called by FXMLLoader
    @FXML
    public void initialize () {

        try {
            String data = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);

            address.setText(data);

        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }

        address.textProperty().addListener((observable, oldValue, newValue) -> {
            update();
        });

        update();

    }

    private void update () {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(Tools.hexStringToByteArray(address.getText()));
            amount = byteBuffer.getLong();
            amountEdit.setText(String.valueOf(amount));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancel (ActionEvent event) {
        overlayUI.done();
    }

    public void send (ActionEvent event) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(Tools.hexStringToByteArray(address.getText()));

        amount = byteBuffer.getLong();
        byteBuffer.get(hash);
        byteBuffer.get(destination);

        Main.thunderContext.makePayment(destination, amount, new PaymentSecret(null, hash), new ResultCommand() {
            @Override
            public void execute (Result result) {
                if(!result.wasSuccessful()) {
                    GuiUtils.informationalAlert("Error", result.getMessage(), null);
                }
            }
        });

        overlayUI.done();

    }
}
