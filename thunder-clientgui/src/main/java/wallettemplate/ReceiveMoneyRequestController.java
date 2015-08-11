/**
 * Sample Skeleton for 'receiver_money_request.fxml' Controller Class
 */

package wallettemplate;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;

import network.thunder.client.api.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import org.bitcoinj.core.Coin;

public class ReceiveMoneyRequestController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="topHBox"
    private HBox topHBox; // Value injected by FXMLLoader

    @FXML // fx:id="titleLabel"
    private Label titleLabel; // Value injected by FXMLLoader

    @FXML // fx:id="labelBtcAmount"
    private Label labelBtcAmount; // Value injected by FXMLLoader

    @FXML // fx:id="btcLabel"
    private Label btcLabel; // Value injected by FXMLLoader

    @FXML // fx:id="FieldUserId"
    private TextField FieldUserId; // Value injected by FXMLLoader

    @FXML // fx:id="FieldHash"
    private TextField FieldHash; // Value injected by FXMLLoader

    @FXML // fx:id="FieldAddress"
    private TextField FieldAddress; // Value injected by FXMLLoader

    @FXML // fx:id="ImageQR"
    private ImageView ImageQR; // Value injected by FXMLLoader

    @FXML // fx:id="cancelBtn"
    private Button cancelBtn; // Value injected by FXMLLoader

    public Main.OverlayUI overlayUI;



    private Coin amount;




    @FXML
    void cancel(ActionEvent event) {

        overlayUI.done();
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert topHBox != null : "fx:id=\"topHBox\" was not injected: check your FXML file 'receiver_money_request.fxml'.";
        assert titleLabel != null : "fx:id=\"titleLabel\" was not injected: check your FXML file 'receiver_money_request.fxml'.";
        assert labelBtcAmount != null : "fx:id=\"labelBtcAmount\" was not injected: check your FXML file 'receiver_money_request.fxml'.";
        assert btcLabel != null : "fx:id=\"btcLabel\" was not injected: check your FXML file 'receiver_money_request.fxml'.";
        assert FieldUserId != null : "fx:id=\"FieldUserId\" was not injected: check your FXML file 'receiver_money_request.fxml'.";
        assert FieldHash != null : "fx:id=\"FieldHash\" was not injected: check your FXML file 'receiver_money_request.fxml'.";
        assert FieldAddress != null : "fx:id=\"FieldAddress\" was not injected: check your FXML file 'receiver_money_request.fxml'.";
        assert ImageQR != null : "fx:id=\"ImageQR\" was not injected: check your FXML file 'receiver_money_request.fxml'.";
        assert cancelBtn != null : "fx:id=\"cancelBtn\" was not injected: check your FXML file 'receiver_money_request.fxml'.";

    }

    public void initData(Coin amount) {

        try {
            PaymentRequest paymentRequest = ThunderContext.getPaymentReceiveRequest(amount.value);

            FieldAddress.setText(paymentRequest.getAddress());

            FieldUserId.setText(paymentRequest.getId());
            FieldHash.setText(paymentRequest.getSecretHash58());


            final byte[] imageBytes = QRCode
                    .from(paymentRequest.getAddress())
                    .withSize(250, 250)
                    .to(ImageType.PNG)
                    .stream()
                    .toByteArray();


            Image qrImage = new Image(new ByteArrayInputStream(imageBytes));
            ImageQR.setImage(qrImage);
            ImageQR.setEffect(new DropShadow());


        } catch (Exception e) {
            e.printStackTrace();
        }

        this.amount = amount;
        labelBtcAmount.setText(amount.toPlainString());
    }



}
