package network.thunder.core.database;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.ChannelStatusObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.lightning.RevocationHash;

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

    Channel getChannel (byte[] nodeKey);

    void saveChannel (Channel channel);

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
