package wallettemplate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class ReceiveMoneyBlockchainController {

    public Main.OverlayUI overlayUI;

    @FXML
    private HBox topHBox;

    @FXML
    private TextField textfieldAmount;

    @FXML
    private Button cancelBtn;

    public void cancel (ActionEvent event) {
        overlayUI.done();
    }

    @FXML
    void initialize () {
        textfieldAmount.setText(Main.wallet.currentReceiveAddress().toString());

    }
}
