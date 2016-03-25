package network.thunder.core.database;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentWrapper;
import org.bitcoinj.core.ECKey;

import java.util.List;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public interface DBHandler {

    //Syncing / Gossiping
    List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex);

    List<P2PDataObject> getSyncDataIPObjects ();

    void insertIPObjects (List<P2PDataObject> ipList);

    List<PubkeyIPObject> getIPObjects ();

    P2PDataObject getP2PDataObjectByHash (byte[] hash);

    PubkeyIPObject getIPObject (byte[] nodeKey);

    void syncDatalist (List<P2PDataObject> dataList);

    //Channels
    void insertRevocationHash (RevocationHash hash);

    RevocationHash createRevocationHash (Channel channel);

    List<RevocationHash> getOldRevocationHashes (Channel channel);

    boolean checkOldRevocationHashes (List<RevocationHash> revocationHashList);

    Channel getChannel (int id);

    List<Channel> getChannel (ECKey nodeKey);

    int saveChannel (Channel channel);

    void updateChannel (Channel channel);

    List<Channel> getOpenChannel ();

    List<PubkeyIPObject> getIPObjectsWithActiveChannel ();

    List<ChannelStatusObject> getTopology ();

    //Payments
    byte[] getSenderOfPayment (PaymentSecret paymentSecret);

    byte[] getReceiverOfPayment (PaymentSecret paymentSecret);

    void addPayment (PaymentWrapper paymentWrapper);

    void updatePayment (PaymentWrapper paymentWrapper);

    void updatePaymentSender (PaymentWrapper paymentWrapper);

    void updatePaymentReceiver (PaymentWrapper paymentWrapper);

    void updatePaymentAddReceiverAddress (PaymentSecret secret, byte[] receiver);

    PaymentWrapper getPayment (PaymentSecret paymentSecret);

    void addPaymentSecret (PaymentSecret secret);

    PaymentSecret getPaymentSecret (PaymentSecret secret);

    //GUI
    List<PaymentWrapper> getAllPayments ();

    List<PaymentWrapper> getOpenPayments ();

    List<PaymentWrapper> getRefundedPayments ();

    List<PaymentWrapper> getRedeemedPayments ();

}
