package network.thunder.core.database;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.DIRECTION;
import network.thunder.core.communication.layer.MessageWrapper;
import network.thunder.core.communication.layer.high.AckableMessage;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.NumberedMessage;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentWrapper;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DBHandler {

    //Messages
    List<MessageWrapper> getMessageList (NodeKey nodeKey, Sha256Hash channelHash, Class c);
    List<AckableMessage> getUnackedMessageList (NodeKey nodeKey);
    NumberedMessage getMessageResponse (NodeKey nodeKey, long messageIdReceived);
    void setMessageAcked (NodeKey nodeKey, long messageId);
    void setMessageProcessed (NodeKey nodeKey, NumberedMessage message);
    long lastProcessedMessaged (NodeKey nodeKey);
    long saveMessage (NodeKey nodeKey, NumberedMessage message, DIRECTION direction);
    void linkResponse (NodeKey nodeKey, long messageRequest, long messageResponse);

    //Syncing / Gossiping
    List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex);
    List<P2PDataObject> getSyncDataIPObjects ();
    void insertIPObjects (List<P2PDataObject> ipList);
    List<PubkeyIPObject> getIPObjects ();
    P2PDataObject getP2PDataObjectByHash (byte[] hash);
    PubkeyIPObject getIPObject (byte[] nodeKey);
    void invalidateP2PObject (P2PDataObject ipObject);
    void syncDatalist (List<P2PDataObject> dataList);

    //Channels
    Channel getChannel (int id);
    Channel getChannel (Sha256Hash hash);

    List<Channel> getChannel (NodeKey nodeKey);
    List<Channel> getOpenChannel (NodeKey nodeKey);
    List<Channel> getOpenChannel ();

    void insertChannel (Channel channel);
    void updateChannelStatus (@NotNull NodeKey nodeKey, @NotNull Sha256Hash channelHash, @NotNull ECKey keyServer,
                              Channel channel, ChannelUpdate update, List<RevocationHash> revocationHash, NumberedMessage request, NumberedMessage response);

    List<PubkeyIPObject> getIPObjectsWithActiveChannel ();
    List<ChannelStatusObject> getTopology ();

    //Payments
    List<PaymentData> lockPaymentsToBeRefunded (NodeKey nodeKey);
    List<PaymentData> lockPaymentsToBeMade (NodeKey nodeKey);
    List<PaymentData> lockPaymentsToBeRedeemed (NodeKey nodeKey);

    void checkPaymentsList ();
    void unlockPayments (NodeKey nodeKey, List<PaymentData> paymentList);

    NodeKey getSenderOfPayment (PaymentSecret paymentSecret);

    void addPayment (NodeKey firstHop, PaymentData paymentWrapper);
    void updatePayment (PaymentWrapper paymentWrapper);
    PaymentWrapper getPayment (PaymentSecret paymentSecret);
    PaymentSecret getPaymentSecret (PaymentSecret secret);
    void addPaymentSecret (PaymentSecret secret);

    //GUI
    List<PaymentWrapper> getAllPayments ();
    List<PaymentWrapper> getOpenPayments ();
    List<PaymentWrapper> getRefundedPayments ();
    List<PaymentWrapper> getRedeemedPayments ();

}
