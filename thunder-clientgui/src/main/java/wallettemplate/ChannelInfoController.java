package wallettemplate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import network.thunder.client.api.ThunderContext;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.Coin;

import java.sql.SQLException;
import java.util.Date;

public class ChannelInfoController {

    public Main.OverlayUI overlayUI;
    @FXML
    private HBox topHBox;
    @FXML
    private Label titleLabel;
    @FXML
    private Label labelOpen;
    @FXML
    private Label labelClose;
    @FXML
    private Label balanceClient;
    @FXML
    private Label balanceClientAcc;
    @FXML
    private Label balanceServer;
    @FXML
    private TextField txOpen;
    @FXML
    private TextField txRefund;
    @FXML
    private TextField txChannel;
    @FXML
    private TextField txRevoke;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button cancelBtn1;

    @FXML
    void cancel (ActionEvent event) {
        overlayUI.done();
    }

    @FXML
    void closeChannel (ActionEvent event) throws Exception {
        overlayUI.done();
        ThunderContext.instance.closeChannel();
    }

    @FXML
        // This method is called by the FXMLLoader when initialization is complete
    void initialize () throws SQLException {
        assert topHBox != null : "fx:id=\"topHBox\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert titleLabel != null : "fx:id=\"titleLabel\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert labelOpen != null : "fx:id=\"labelOpen\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert labelClose != null : "fx:id=\"labelClose\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert balanceClient != null : "fx:id=\"balanceClient\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert balanceClientAcc != null : "fx:id=\"balanceClientAcc\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert balanceServer != null : "fx:id=\"balanceServer\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert txOpen != null : "fx:id=\"txOpen\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert txRefund != null : "fx:id=\"txRefund\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert txChannel != null : "fx:id=\"txChannel\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert txRevoke != null : "fx:id=\"txRevoke\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert cancelBtn != null : "fx:id=\"cancelBtn\" was not injected: check your FXML file 'channel_info.fxml'.";
        assert cancelBtn1 != null : "fx:id=\"cancelBtn1\" was not injected: check your FXML file 'channel_info.fxml'.";

        Channel channel = ThunderContext.instance.getCurrentChannel();

        balanceClient.setText(Coin.valueOf(channel.getAmountClient()).toFriendlyString());
        balanceServer.setText(Coin.valueOf(channel.getAmountServer()).toFriendlyString());
        balanceClientAcc.setText(ThunderContext.instance.getAmountClientAccessible().toFriendlyString());

        labelClose.setText(new Date(((long) channel.getTimestampClose()) * 1000).toString());
        labelOpen.setText(new Date(((long) channel.getTimestampOpen()) * 1000).toString());

        txOpen.setText(Tools.bytesToHex(channel.getOpeningTx().bitcoinSerialize()));
        txRefund.setText(Tools.bytesToHex(channel.getRefundTxClient().bitcoinSerialize()));

        if (channel.getChannelTxClientID() != 0) {
            txChannel.setText(Tools.bytesToHex(channel.getChannelTxClient().bitcoinSerialize()));
            txRevoke.setText(Tools.bytesToHex(channel.getChannelTxRevokeClient().bitcoinSerialize()));
        }

    }

}
