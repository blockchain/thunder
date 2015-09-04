package wallettemplate;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.bitcoinj.core.Coin;
import wallettemplate.utils.GuiUtils;

import java.io.IOException;
import java.net.URL;

/**
 * Created by PC on 27.07.2015.
 */
public class ReceiveMoneyController {
	public HBox topHBox;
	public Label titleLabel;
	public TextField amountEdit;
	public Label btcLabel;
	public Button cancelBtn;
	public Button sendBtn;

	public Main.OverlayUI overlayUI;

	public void cancel (ActionEvent event) {
		overlayUI.done();
	}

	public void send (ActionEvent actionEvent) {

		try {
			URL location = GuiUtils.getResource("receive_money_request.fxml");
			FXMLLoader loader = new FXMLLoader(location);
			Pane ui = loader.load();

			ReceiveMoneyRequestController controller = loader.getController();

			try {
				controller.initData(Coin.parseCoin(amountEdit.getText()));
			} catch (NumberFormatException e) {
				controller.initData(Coin.valueOf(1000));
			}

			Main.instance.overlayUI(ui, controller);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
