/**
 * Sample Skeleton for 'receiver_money_request.fxml' Controller Class
 */

package wallettemplate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import network.thunder.client.api.PaymentRequest;
import network.thunder.client.api.ThunderContext;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.Coin;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class ReceiveMoneyRequestController {

	public Main.OverlayUI overlayUI;
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
	@FXML
	private TextField FieldRequest;
	private Coin amount;

	@FXML
	void cancel (ActionEvent event) {

		overlayUI.done();
	}

	public void initData (Coin amount) {

		try {
			PaymentRequest paymentRequest = ThunderContext.instance.getPaymentReceiveRequest(amount.value);

			FieldAddress.setText(paymentRequest.getAddress());

			FieldUserId.setText(Tools.byteToString(paymentRequest.getId()));
			FieldHash.setText(Tools.byteToString(paymentRequest.getSecretHash()));
			FieldRequest.setText(paymentRequest.getPaymentURI());

			final byte[] imageBytes = QRCode.from(paymentRequest.getAddress()).withSize(250, 250).to(ImageType.PNG).stream().toByteArray();

			Image qrImage = new Image(new ByteArrayInputStream(imageBytes));
			ImageQR.setImage(qrImage);
			ImageQR.setEffect(new DropShadow());

		} catch (Exception e) {
			e.printStackTrace();
		}

		this.amount = amount;
		labelBtcAmount.setText(amount.toPlainString());
	}

	@FXML
		// This method is called by the FXMLLoader when initialization is complete
	void initialize () {
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

}
