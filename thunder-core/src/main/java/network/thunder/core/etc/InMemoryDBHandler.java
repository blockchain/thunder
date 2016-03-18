package network.thunder.core.etc;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.ChannelStatusObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyChannelObject;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.PubkeyIPObject;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.lightning.RevocationHash;

import java.util.*;

/**
 * Created by matsjerratsch on 04/12/2015.
 */
public class InMemoryDBHandler implements DBHandler {

    public List<Channel> channelList = Collections.synchronizedList(new ArrayList<>());

    public List<PubkeyIPObject> pubkeyIPList = Collections.synchronizedList(new ArrayList<>());
    public List<PubkeyChannelObject> pubkeyChannelList = Collections.synchronizedList(new ArrayList<>());
    public List<ChannelStatusObject> channelStatusList = Collections.synchronizedList(new ArrayList<>());

    public Map<Integer, List<P2PDataObject>> fragmentToListMap = new HashMap<>();

    public List<P2PDataObject> totalList = Collections.synchronizedList(new ArrayList<>());

    public List<PubkeyIPObject> pubkeyIPObjectOpenChannel = Collections.synchronizedList(new ArrayList<>());

    public List<RevocationHash> revocationHashListTheir = Collections.synchronizedList(new ArrayList<>());
    public List<RevocationHash> revocationHashListOurs = Collections.synchronizedList(new ArrayList<>());

    List<PaymentWrapper> payments = new ArrayList<>();
    List<PaymentSecret> secrets = new ArrayList<>();

    public InMemoryDBHandler () {
        for (int i = 0; i < 1001; i++) {
            fragmentToListMap.put(i, Collections.synchronizedList(new ArrayList<>()));
        }
    }

    @Override
    public void insertIPObjects (List<P2PDataObject> ipList) {
        syncDatalist(ipList);
    }

    @Override
    public List<PubkeyIPObject> getIPObjects () {
        return new ArrayList<>(pubkeyIPList);
    }

    @Override
    public P2PDataObject getP2PDataObjectByHash (byte[] hash) {
        for (P2PDataObject object : totalList) {
            if (Arrays.equals(object.getHash(), hash)) {
                return object;
            }
        }
        return null;
    }

    @Override
    public PubkeyIPObject getIPObject (byte[] nodeKey) {
        for (PubkeyIPObject p : pubkeyIPList) {
            if (Arrays.equals(p.pubkey, nodeKey)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void syncDatalist (List<P2PDataObject> dataList) {
        for (P2PDataObject obj : dataList) {
            fragmentToListMap.get(obj.getFragmentIndex()).add(obj);
            if (obj instanceof PubkeyIPObject) {
                if (!pubkeyIPList.contains(obj)) {
                    pubkeyIPList.add((PubkeyIPObject) obj);
                }
            } else if (obj instanceof PubkeyChannelObject) {
                if (!pubkeyChannelList.contains(obj)) {
                    pubkeyChannelList.add((PubkeyChannelObject) obj);
                }
            } else if (obj instanceof ChannelStatusObject) {

                ChannelStatusObject temp = (ChannelStatusObject) obj;
                boolean found = false;
                for (ChannelStatusObject object : channelStatusList) {
                    if (Arrays.equals(object.pubkeyA, temp.pubkeyA) && Arrays.equals(object.pubkeyB, temp.pubkeyB)) {
                        found = true;
                        break;
                    }

                    if (Arrays.equals(object.pubkeyB, temp.pubkeyA) && Arrays.equals(object.pubkeyA, temp.pubkeyB)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }

                if (!channelStatusList.contains(obj)) {
                    channelStatusList.add((ChannelStatusObject) obj);
                }
            }
            List<P2PDataObject> list = getSyncDataByFragmentIndex(obj.getFragmentIndex());
            if (!list.contains(obj)) {
                list.add(obj);
            }
            if (!totalList.contains(obj)) {
                totalList.add(obj);
            }
        }
    }

    @Override
    public void insertRevocationHash (RevocationHash hash) {
        revocationHashListTheir.add(hash);
    }

    @Override
    public RevocationHash createRevocationHash (Channel channel) {
        RevocationHash hash = new RevocationHash(1, 1, Tools.getRandomByte(32));
        revocationHashListOurs.add(hash);
        return hash;
    }

    @Override
    public List<RevocationHash> getOldRevocationHashes (Channel channel) {
        List<RevocationHash> hashList = new ArrayList<>(revocationHashListOurs);
        revocationHashListOurs.clear();
        return hashList;
    }

    @Override
    public boolean checkOldRevocationHashes (List<RevocationHash> revocationHashList) {
        //TODO
        return true;
    }

    @Override
    public Channel getChannel (byte[] nodeKey) {
        for (Channel channel : channelList) {
            if (Arrays.equals(channel.nodeId, nodeKey)) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public void saveChannel (Channel channel) {
        this.channelList.add(channel);
    }

    @Override
    public void updateChannel (Channel channel) {
        Iterator<Channel> iterator = channelList.iterator();
        while (iterator.hasNext()) {
            Channel c = iterator.next();
            if (Arrays.equals(c.nodeId, channel.nodeId)) {
                iterator.remove();
                channelList.add(channel);
                return;
            }
        }
        throw new RuntimeException("Not able to find channel in list, not updated..");
    }

    @Override
    public List<Channel> getOpenChannel () {
        return channelList;
    }

    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
        return fragmentToListMap.get(fragmentIndex);
    }

    @Override
    public List<P2PDataObject> getSyncDataIPObjects () {
        return null;
    }

    @Override
    public List<PubkeyIPObject> getIPObjectsWithActiveChannel () {
        return new ArrayList<>();
    }

    @Override
    public List<ChannelStatusObject> getTopology () {
        List<ChannelStatusObject> list = new ArrayList<>();
        for (P2PDataObject object : totalList) {
            if (object instanceof ChannelStatusObject) {
                list.add((ChannelStatusObject) object);
            }
        }
        return list;
    }

    @Override
    public List<PaymentWrapper> getAllPayments () {
        return new ArrayList<>();
    }

    @Override
    public List<PaymentWrapper> getOpenPayments () {
        return new ArrayList<>();
    }

    @Override
    public List<PaymentWrapper> getRefundedPayments () {
        return new ArrayList<>();
    }

    @Override
    public List<PaymentWrapper> getRedeemedPayments () {
        return new ArrayList<>();
    }

    @Override
    public void addPayment (PaymentWrapper paymentWrapper) {
        if (payments.contains(paymentWrapper)) {
            return;
//            throw new RuntimeException("Double payment added?");
        }
        payments.add(paymentWrapper);
    }

    @Override
    public void updatePayment (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.receiver = paymentWrapper.receiver;
                p.sender = paymentWrapper.sender;
                p.statusReceiver = paymentWrapper.statusReceiver;
                p.statusSender = paymentWrapper.statusSender;
            }
        }
    }

    @Override
    public void updatePaymentSender (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.statusSender = paymentWrapper.statusSender;
            }
        }
    }

    @Override
    public void updatePaymentReceiver (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.statusReceiver = paymentWrapper.statusReceiver;
            }
        }
    }

    @Override
    public void updatePaymentAddReceiverAddress (PaymentSecret secret, byte[] receiver) {
        for (PaymentWrapper p : payments) {
            if (p.paymentData.secret.equals(secret)) {
                p.receiver = receiver;
            }
        }
    }

    @Override
    public PaymentWrapper getPayment (PaymentSecret paymentSecret) {
        System.out.println(payments.size());

        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment;
            }
        }
        return null;
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {
        if (secrets.contains(secret)) {
            PaymentSecret oldSecret = secrets.get(secrets.indexOf(secret));
            oldSecret.secret = secret.secret;
        } else {
            secrets.add(secret);
        }
    }

    @Override
    public PaymentSecret getPaymentSecret (PaymentSecret secret) {
        if (!secrets.contains(secret)) {
            return null;
        }
        return secrets.get(secrets.indexOf(secret));
    }

    @Override
    public byte[] getSenderOfPayment (PaymentSecret paymentSecret) {
        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment.sender;
            }
        }
        return null;
    }

    @Override
    public byte[] getReceiverOfPayment (PaymentSecret paymentSecret) {
        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment.receiver;
            }
        }
        return null;
    }

    private void pruneLists () {
        List<PubkeyIPObject> oldList = new ArrayList<>();

        for (PubkeyIPObject oldIP : pubkeyIPList) {
            for (PubkeyIPObject newIP : pubkeyIPList) {
                if (!oldIP.equals(newIP)) {
                    if (Arrays.equals(oldIP.pubkey, newIP.pubkey) && (oldIP.timestamp < newIP.timestamp)) {
                        oldList.add(oldIP);
                    }
                }
            }
        }

        pubkeyIPList.removeAll(oldList);
    }
}
