package network.thunder.core.etc;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
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
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.ChannelSettlement;
import network.thunder.core.database.objects.PaymentWrapper;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DBHandlerMock implements DBHandler {
    @Override
    public int getLastBlockHeight () {
        return 0;
    }

    @Override
    public void updateLastBlockHeight (int blockHeight) {

    }

    @Override
    public String getNetwork () {
        return null;
    }

    @Override
    public void setNetwork (String network) {

    }

    @Override
    public ServerObject getServerObject () {
        return null;
    }

    @Override
    public List<MessageWrapper> getMessageList (NodeKey nodeKey, Sha256Hash channelHash, String classType) {
        return null;
    }

    @Override
    public List<AckableMessage> getUnackedMessageList (NodeKey nodeKey) {
        return null;
    }

    @Override
    public AckableMessage getMessageResponse (NodeKey nodeKey, long messageIdReceived) {
        return null;
    }

    @Override
    public void setMessageAcked (NodeKey nodeKey, long messageId) {

    }

    @Override
    public void setMessageProcessed (NodeKey nodeKey, NumberedMessage message) {

    }


    @Override
    public long lastProcessedMessaged (NodeKey nodeKey) {
        return 0;
    }

    @Override
    public long insertMessage (NodeKey nodeKey, NumberedMessage message, DIRECTION direction) {
        return 0;
    }


    @Override
    public void linkResponse (NodeKey nodeKey, long messageRequest, long messageResponse) {

    }

    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
        return null;
    }


    @Override
    public void insertIPObjects (List<P2PDataObject> ipList) {

    }

    @Override
    public List<PubkeyIPObject> getIPObjects () {
        return null;
    }

    @Override
    public P2PDataObject getP2PDataObjectByHash (byte[] hash) {
        return null;
    }

    @Override
    public PubkeyIPObject getIPObject (byte[] nodeKey) {
        return null;
    }

    @Override
    public void invalidateP2PObject (P2PDataObject ipObject) {

    }

    @Override
    public void syncDatalist (List<P2PDataObject> dataList) {

    }


    @Override
    public Channel getChannel (Sha256Hash hash) {
        return null;
    }

    @Override
    public List<Channel> getChannel () {
        return null;
    }

    @Override
    public List<Channel> getChannel (NodeKey nodeKey) {
        return null;
    }

    @Override
    public List<Channel> getOpenChannel (NodeKey nodeKey) {
        return null;
    }

    @Override
    public List<Channel> getOpenChannel () {
        return null;
    }

    @Override
    public void insertChannel (Channel channel) {

    }

    @Override
    public void updateChannelStatus (@NotNull NodeKey nodeKey, @NotNull Sha256Hash channelHash, @NotNull ECKey keyServer, Channel channel, ChannelUpdate update, List<RevocationHash> revocationHash, NumberedMessage request, NumberedMessage response) {

    }

    @Override
    public void updateChannel (Channel channel) {

    }

    @Override
    public RevocationHash retrieveRevocationHash (Sha256Hash channelHash, int shaChainDepth) {
        return null;
    }

    @Override
    public List<PubkeyIPObject> getIPObjectsWithActiveChannel () {
        return null;
    }

    @Override
    public List<ChannelStatusObject> getTopology () {
        return null;
    }

    @Override
    public List<PaymentData> lockPaymentsToBeRefunded (NodeKey nodeKey) {
        return null;
    }

    @Override
    public List<PaymentData> lockPaymentsToBeMade (NodeKey nodeKey) {
        return null;
    }

    @Override
    public List<PaymentData> lockPaymentsToBeRedeemed (NodeKey nodeKey) {
        return null;
    }

    @Override
    public void unlockPayments (Sha256Hash channelHash) {

    }

    @Override
    public NodeKey getSenderOfPayment (PaymentSecret paymentSecret) {
        return null;
    }

    @Override
    public int insertPayment (NodeKey firstHop, PaymentData paymentData) {
        return 0;
    }

    @Override
    public void updatePayment (PaymentData paymentData) {

    }

    @Override
    public PaymentData getPayment (int paymentId) {
        return null;
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {

    }

    @Override
    public List<ChannelSettlement> getSettlements (Sha256Hash channelHash) {
        return null;
    }

    @Override
    public void addPaymentSettlement (ChannelSettlement settlement) {

    }

    @Override
    public void updatePaymentSettlement (ChannelSettlement settlement) {

    }

    @Override
    public PaymentSecret getPaymentSecret (PaymentSecret secret) {
        return null;
    }

    @Override
    public List<PaymentWrapper> getAllPayments () {
        return null;
    }

    @Override
    public List<PaymentData> getAllPayments (Sha256Hash channelHash) {
        return null;
    }

    @Override
    public List<PaymentWrapper> getOpenPayments () {
        return null;
    }

    @Override
    public List<PaymentWrapper> getRefundedPayments () {
        return null;
    }

    @Override
    public List<PaymentWrapper> getRedeemedPayments () {
        return null;
    }
}
