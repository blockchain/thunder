package wallettemplate;

import network.thunder.*;
import network.thunder.client.api.ThunderContext;import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.bitcoinj.core.Coin;
import wallettemplate.utils.BitcoinUIModel;

public class CreateChannelController {

    public Main.OverlayUI overlayUI;

    @FXML
    private HBox topHBox;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField amountEdit;

    @FXML
    private Label btcLabel;

    @FXML
    private HBox topHBox1;

    @FXML
    private Label titleLabel1;

    @FXML
    private TextField amountEdit1;

    @FXML
    private Label btcLabel1;

    @FXML
    private HBox topHBox2;

    @FXML
    private Label titleLabel2;

    @FXML
    private TextField amountEdit2;

    @FXML
    private Label btcLabel2;

    @FXML
    private Button cancelBtn;

    @FXML
    private Button sendBtn;

    @FXML
    void cancel(ActionEvent event) {
        overlayUI.done();
    }

    @FXML
    void send(ActionEvent event) {
        Main.instance.notificationBar.pushItem("Open Channel..", BitcoinUIModel.syncProgress);

        overlayUI.done();
        try {
            try {
                Coin c1 = Coin.parseCoin(amountEdit.getText());
                Coin c2 = Coin.parseCoin(amountEdit1.getText());
                int days = Integer.parseInt(amountEdit2.getText());
                ThunderContext.instance.openChannel(c1.getValue(), c2.getValue(), days);
            } catch(NumberFormatException e) {
                ThunderContext.instance.openChannel(10000, 10000, 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
