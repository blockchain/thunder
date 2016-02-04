package network.thunder.core.etc;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.lightning.RevocationHash;
import network.thunder.core.mesh.Node;

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
    public Channel getChannel (Node node) {
        return null;
    }

    @Override
    public List<PubkeyIPObject> getIPObjectsWithActiveChannel () {
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
}
