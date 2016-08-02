package network.thunder.core.helper;

import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.high.channel.ChannelManager;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogicImpl;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.persistent.SQLDBHandler;
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

    DBHandler dbHandler = new SQLDBHandler(TestTools.getH2InMemoryDataSource());

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

    private List<P2PDataObject> getTotalSyncData (DBHandler dbHandler) {
        List<P2PDataObject> list = new ArrayList<>();
        for (int i = 0; i <= P2PDataObject.NUMBER_OF_FRAGMENTS; ++i) {
            list.addAll(dbHandler.getSyncDataByFragmentIndex(i));
        }
        return list;
    }

}