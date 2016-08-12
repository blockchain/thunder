package network.thunder.core.helper;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogicImpl;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.PaymentStatus;
import network.thunder.core.etc.TestTools;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.blockchain.MockBlockchainHelper;
import org.bitcoinj.core.Transaction;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DBHandlerTest {
    private static final Logger log = Tools.getLogger();

    LNPaymentLogic paymentLogic = new LNPaymentLogicImpl();

    ServerObject serverObject1 = new ServerObject();
    ServerObject serverObject2 = new ServerObject();

    ClientObject node1 = new ClientObject(serverObject2);
    ClientObject node2 = new ClientObject(serverObject1);

    DBHandler dbHandler = TestTools.getTestDBHandler();

    MockBlockchainHelper blockchainHelper1 = new MockBlockchainHelper();
    MockBlockchainHelper blockchainHelper2 = new MockBlockchainHelper();

    ChannelManager channelManager1;
    ChannelManager channelManager2;

    Transaction channelTransaction;

    long startBalance1;
    long startBalance2;

    long diffBalance1;
    long diffBalance2;

    long paymentValue = 10000;

    Set<Transaction> totalTx = new HashSet<>();

    @Test
    public void shouldInsertIPObject () throws Exception {
        List<P2PDataObject> dataList = new ArrayList<>();
        dataList.add(TestTools.getRandomObjectIpObject());
        dbHandler.syncDatalist(dataList);

        List<P2PDataObject> list = getTotalSyncData(dbHandler);

        assertEquals(list.size(), 1);
        assertEquals(list.get(0), dataList.get(0));
    }

    @Test
    public void shouldInsertChannelObject () throws Exception {
        List<P2PDataObject> dataList = new ArrayList<>();
        dataList.add(TestTools.getRandomObjectIpObject());
        dbHandler.syncDatalist(dataList);

        List<P2PDataObject> list = getTotalSyncData(dbHandler);

        assertEquals(list.size(), 1);
        assertEquals(list.get(0), dataList.get(0));
    }

    @Test
    public void shouldInsertChannel () throws Exception {
        Channel channel = TestTools.getMockChannel(new LNConfiguration());
        dbHandler.insertChannel(channel);

        Channel channelFromDatabase = dbHandler.getChannel(channel.getHash());

        assertEquals(channel, channelFromDatabase);
    }

    @Test
    public void shouldUpdateChannel () throws Exception {
        Channel channel = TestTools.getMockChannel(new LNConfiguration());
        dbHandler.insertChannel(channel);

        //Hash is determined by both keys
        Channel randomUpdate = TestTools.getMockChannel(new LNConfiguration());
        randomUpdate.keyClient = channel.keyClient;
        randomUpdate.keyServer = channel.keyServer;

        //Updating the NodeKey is not supported for now
        randomUpdate.nodeKeyClient = channel.nodeKeyClient;

        //Change fields that are the same else
        randomUpdate.amountServer = 11111;
        randomUpdate.amountClient = 8889;
        randomUpdate.timestampForceClose = 1;
        randomUpdate.timestampOpen = 2;
        randomUpdate.anchorBlockHeight = 3;
        randomUpdate.feePerByte = 10;
        randomUpdate.shaChainDepthCurrent = 3;
        randomUpdate.revoHashClientCurrent = new RevocationHash(3, Tools.getRandomByte(20));
        randomUpdate.revoHashClientNext = new RevocationHash(4, Tools.getRandomByte(20));
        randomUpdate.revoHashServerCurrent = new RevocationHash(3, randomUpdate.masterPrivateKeyServer);
        randomUpdate.revoHashServerNext = new RevocationHash(4, randomUpdate.masterPrivateKeyServer);

        dbHandler.updateChannel(randomUpdate);

        channel = dbHandler.getChannel(channel.getHash());

        assertEquals(channel, randomUpdate);
    }

    @Test
    public void shouldInsertPayment () throws Exception {
        Channel channel = TestTools.getMockChannel(new LNConfiguration());
        channel.nodeKeyClient = node1.nodeKey;
        channel.phase = Channel.Phase.OPEN;
        dbHandler.insertChannel(channel);


        PaymentData paymentData = TestTools.getMockPaymentData(serverObject1.pubKeyServer, serverObject2.pubKeyServer);

        dbHandler.insertPayment(node1.nodeKey, paymentData);

        PaymentData paymentFromDatabase = dbHandler.getPayment(paymentData.paymentId);

        assertEquals(paymentData, paymentFromDatabase);
    }

    @Test
    public void shouldUpdatePayment () throws Exception {
        Channel channel = TestTools.getMockChannel(new LNConfiguration());
        channel.nodeKeyClient = node1.nodeKey;
        channel.phase = Channel.Phase.OPEN;
        dbHandler.insertChannel(channel);

        PaymentData paymentData = TestTools.getMockPaymentData(serverObject1.pubKeyServer, serverObject2.pubKeyServer);
        PaymentData randomUpdate = TestTools.getMockPaymentData(serverObject1.pubKeyServer, serverObject2.pubKeyServer);

        dbHandler.insertPayment(node1.nodeKey, paymentData);

        randomUpdate.paymentId = paymentData.paymentId;
        randomUpdate.amount = 1111;
        randomUpdate.status = PaymentStatus.REFUNDED;
        randomUpdate.timestampOpen = 1;
        randomUpdate.timestampSettled = 2;
        randomUpdate.timestampRefund = 3;

        dbHandler.updatePayment(randomUpdate);

        PaymentData paymentFromDatabase = dbHandler.getPayment(paymentData.paymentId);

        assertEquals(randomUpdate, paymentFromDatabase);
    }

    private List<P2PDataObject> getTotalSyncData (DBHandler dbHandler) {
        List<P2PDataObject> list = new ArrayList<>();
        for (int i = 0; i <= P2PDataObject.NUMBER_OF_FRAGMENTS; ++i) {
            list.addAll(dbHandler.getSyncDataByFragmentIndex(i));
        }
        return list;
    }

}