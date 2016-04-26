package network.thunder.core.etc;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.PaymentWrapper;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

import java.util.List;

/**
 * Created by matsjerratsch on 13/01/2016.
 */
public class DBHandlerMock implements DBHandler {
    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
        return null;
    }

    @Override
    public List<P2PDataObject> getSyncDataIPObjects () {
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
    public void syncDatalist (List<P2PDataObject> dataList) {

    }

    @Override
    public void insertRevocationHash (RevocationHash hash) {

    }

    @Override
    public RevocationHash createRevocationHash (Channel channel) {
        return null;
    }

    @Override
    public List<RevocationHash> getOldRevocationHashes (Channel channel) {
        return null;
    }

    @Override
    public boolean checkOldRevocationHashes (List<RevocationHash> revocationHashList) {
        return true;
    }

    @Override
    public Channel getChannel (int id) {
        return null;
    }

    @Override
    public Channel getChannel (Sha256Hash hash) {
        return null;
    }

    @Override
    public List<Channel> getChannel (ECKey nodeKey) {
        return null;
    }

    @Override
    public int saveChannel (Channel channel) {
        return 0;
    }

    @Override
    public void updateChannel (Channel channel) {

    }

    @Override
    public List<Channel> getOpenChannel () {
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
    public byte[] getSenderOfPayment (PaymentSecret paymentSecret) {
        return new byte[0];
    }

    @Override
    public byte[] getReceiverOfPayment (PaymentSecret paymentSecret) {
        return new byte[0];
    }

    @Override
    public void addPayment (PaymentWrapper paymentWrapper) {

    }

    @Override
    public void updatePayment (PaymentWrapper paymentWrapper) {

    }

    @Override
    public void updatePaymentSender (PaymentWrapper paymentWrapper) {

    }

    @Override
    public void updatePaymentReceiver (PaymentWrapper paymentWrapper) {

    }

    @Override
    public void updatePaymentAddReceiverAddress (PaymentSecret secret, byte[] receiver) {

    }

    @Override
    public PaymentWrapper getPayment (PaymentSecret paymentSecret) {
        return null;
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {

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
