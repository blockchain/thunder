/**
 * Sample Skeleton for 'receiver_money_request.fxml' Controller Class
 */

package wallettemplate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.etc.Tools;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ResourceBundle;

public class ReceiveMoneyRequestController {

    public Main.OverlayUI overlayUI;

    @FXML
    private ResourceBundle resources;

    @FXML
    private HBox topHBox;

    @FXML
    private TextField amount;

    @FXML
    private TextField FieldHash;

    @FXML
    private TextArea FieldAddress;

    @FXML
    private TextField FieldRequest;

    @FXML
    private ImageView ImageQR;

    @FXML
    private Button cancelBtn;

    @FXML
    void cancel (ActionEvent event) {
        overlayUI.done();
    }

    PaymentSecret secret = null;

    @FXML
    void initialize () {
        assert topHBox != null : "fx:id=\"topHBox\" was not injected: check your FXML file 'receive_money_request.fxml'.";
        assert amount != null : "fx:id=\"amount\" was not injected: check your FXML file 'receive_money_request.fxml'.";
        assert FieldHash != null : "fx:id=\"FieldHash\" was not injected: check your FXML file 'receive_money_request.fxml'.";
        assert FieldAddress != null : "fx:id=\"FieldAddress\" was not injected: check your FXML file 'receive_money_request.fxml'.";
        assert FieldRequest != null : "fx:id=\"FieldRequest\" was not injected: check your FXML file 'receive_money_request.fxml'.";
        assert ImageQR != null : "fx:id=\"ImageQR\" was not injected: check your FXML file 'receive_money_request.fxml'.";
        assert cancelBtn != null : "fx:id=\"cancelBtn\" was not injected: check your FXML file 'receive_money_request.fxml'.";

        amount.textProperty().addListener((observable, oldValue, newValue) -> {
            update();
        });
        update();
    }

    public void update () {

        System.out.println(Tools.bytesToHex(Main.node.pubKeyServer.getPubKey()));

        if (secret == null) {
            secret = new PaymentSecret(Tools.getRandomByte(20));
            Main.dbHandler.addPaymentSecret(secret);
            System.out.println("HASH: "+Tools.bytesToHex(secret.hash));
        }

        try {

            byte[] payload = getPayload();

            FieldAddress.setText(Tools.bytesToHex(payload));
            FieldHash.setText(Tools.bytesToHex(secret.hash));

            System.out.println(Tools.bytesToHex(payload));

            final byte[] imageBytes = QRCode
                    .from(Tools.bytesToHex(payload))
                    .withSize(250, 250)
                    .to(ImageType.PNG)
                    .stream()
                    .toByteArray();

            Image qrImage = new Image(new ByteArrayInputStream(imageBytes));
            ImageQR.setImage(qrImage);
            ImageQR.setEffect(new DropShadow());

            StringSelection stringSelection = new StringSelection(Tools.bytesToHex(payload));
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public byte[] getPayload () {

        ByteBuffer buffer = ByteBuffer.allocate(33 + 8 + 20);

        buffer.putLong(getAmount());
        buffer.put(secret.hash);
        buffer.put(Main.node.pubKeyServer.getPubKey());

        return buffer.array();
    }

    private long getAmount () {
        return Long.parseLong(amount.getText());
    }

}
