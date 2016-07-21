package network.thunder.core.communication.layers.high;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.ProcessorHandler;
import network.thunder.core.communication.layer.high.AckMessageImpl;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.*;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentAMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentBMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentCMessage;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.InMemoryDBHandler;
import network.thunder.core.etc.MockContextFactory;
import network.thunder.core.etc.TestTools;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

public class LNPaymentHandlerTest {

    EmbeddedChannel channel12;
    EmbeddedChannel channel21;

    ServerObject serverObject1 = new ServerObject();
    ServerObject serverObject2 = new ServerObject();

    ClientObject node1 = new ClientObject(serverObject2);
    ClientObject node2 = new ClientObject(serverObject1);

    LNPaymentProcessorImpl processor12;
    LNPaymentProcessorImpl processor21;

    DBHandler dbHandler1 = new InMemoryDBHandler();
    DBHandler dbHandler2 = new InMemoryDBHandler();

    ContextFactory contextFactory12 = new MockContextFactory(serverObject1, dbHandler1);
    ContextFactory contextFactory21 = new MockContextFactory(serverObject2, dbHandler2);

    LNPaymentHelper paymentHelper1 = contextFactory12.getPaymentHelper();
    LNPaymentHelper paymentHelper2 = contextFactory21.getPaymentHelper();

    Channel channel1 = TestTools.getMockChannel(new LNConfiguration());
    Channel channel2 = TestTools.getMockChannel(new LNConfiguration());

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        node1.isServer = false;
        node2.isServer = true;

        channel1.nodeKeyClient = new NodeKey(serverObject2.pubKeyServer);
        channel2.nodeKeyClient = new NodeKey(serverObject1.pubKeyServer);

        channel1.retrieveDataFromOtherChannel(channel2);
        channel2.retrieveDataFromOtherChannel(channel1);
        channel2.amountServer = channel1.amountClient;
        channel2.amountClient = channel1.amountServer;

        dbHandler1.insertChannel(channel1);
        dbHandler2.insertChannel(channel2);

        this.node1.name = "LNPayment12";
        this.node2.name = "LNPayment21";

        processor12 = new LNPaymentProcessorImpl(contextFactory12, dbHandler1, this.node1);
        processor21 = new LNPaymentProcessorImpl(contextFactory21, dbHandler2, this.node2);

        channel12 = new EmbeddedChannel(new ProcessorHandler(processor12, "LNPayment12"));
        channel21 = new EmbeddedChannel(new ProcessorHandler(processor21, "LNPayment21"));

        Message m = (Message) channel21.readOutbound();
        assertNull(m);

    }

    public void after () {
        channel12.checkException();
        channel21.checkException();
    }

    @Test
    public void fullExchange () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        PaymentData paymentData = getMockPaymentData(serverObject1.pubKeyServer, serverObject2.pubKeyServer);
        dbHandler2.addPaymentSecret(paymentData.secret);
        paymentHelper1.makePayment(paymentData);

        TestTools.exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel21, channel12, AckMessageImpl.class);

        TestTools.exchangeMessages(channel21, channel12, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentBMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel12, channel21, AckMessageImpl.class);

        assertNull(channel12.readOutbound());
        assertNull(channel21.readOutbound());

        Channel channel1After = dbHandler1.getChannel(channel1.getHash());
        Channel channel2After = dbHandler2.getChannel(channel2.getHash());

        assertEquals(channel1.amountServer - paymentData.amount, channel1After.amountServer);
        assertEquals(channel1.amountClient + paymentData.amount, channel1After.amountClient);
        assertEquals(channel2.amountServer + paymentData.amount, channel2After.amountServer);
        assertEquals(channel2.amountClient - paymentData.amount, channel2After.amountClient);

        after();
    }

    @Test
    public void fullExchangeWithAnotherPaymentMidway () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        paymentHelper1.makePayment(getMockPaymentData(serverObject1.pubKeyServer, serverObject2.pubKeyServer));

        TestTools.exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        paymentHelper1.makePayment(getMockPaymentData(serverObject1.pubKeyServer, serverObject2.pubKeyServer));
        TestTools.exchangeMessages(channel12, channel21, LNPaymentCMessage.class);

        assertTrue(channel12.readOutbound() instanceof LNPaymentAMessage);

        after();
    }

    @Test
    public void fullExchangeConflict () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        PaymentData payment1 = getMockPaymentData(serverObject1.pubKeyServer, serverObject2.pubKeyServer);
        PaymentData payment2 = getMockPaymentData(serverObject2.pubKeyServer, serverObject1.pubKeyServer);

        paymentHelper1.makePayment(payment1);
        paymentHelper2.makePayment(payment2);

        dbHandler1.addPaymentSecret(payment2.secret);
        dbHandler2.addPaymentSecret(payment1.secret);

        LNPaymentAMessage messageA1 = (LNPaymentAMessage) channel12.readOutbound();
        LNPaymentAMessage messageA2 = (LNPaymentAMessage) channel21.readOutbound();

        channel21.writeInbound(messageA1);
        channel12.writeInbound(messageA2);

        if (messageA1.dice > messageA2.dice) {
            EmbeddedChannel temp = channel12;
            channel12 = channel21;
            channel21 = temp;
        }

        TestTools.exchangeMessages(channel12, channel21, AckMessageImpl.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentBMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentCMessage.class);

        TestTools.exchangeMessages(channel12, channel21, AckMessageImpl.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentCMessage.class);

        after();
    }

    @Test
    public void sendWrongMessageShouldDisconnect () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        paymentHelper1.makePayment(getMockPaymentData(serverObject1.pubKeyServer, serverObject2.pubKeyServer));

        Thread.sleep(100);
        LNPaymentAMessage messageA = (LNPaymentAMessage) channel12.readOutbound();
        channel21.writeInbound(messageA);
        LNPaymentBMessage messageB = (LNPaymentBMessage) channel21.readOutbound();
        channel21.writeInbound(messageB);

        assertFalse(channel21.isOpen());

        after();
    }

    public PaymentData getMockPaymentData (ECKey key1, ECKey key2) {
        LNConfiguration configuration = new LNConfiguration();
        PaymentData paymentData = new PaymentData();
        paymentData.sending = true;
        paymentData.amount = 10000;
        paymentData.secret = new PaymentSecret(Tools.getRandomByte(20));
        paymentData.timestampOpen = Tools.currentTime();
        paymentData.timestampRefund = paymentData.timestampOpen + configuration.DEFAULT_REFUND_DELAY * 10;

        LNOnionHelper onionHelper = new LNOnionHelperImpl();
        List<byte[]> route = new ArrayList<>();
        route.add(key1.getPubKey());
        route.add(key2.getPubKey());

        paymentData.onionObject = onionHelper.createOnionObject(route, null);

        return paymentData;
    }

    public void connectChannel (EmbeddedChannel from, EmbeddedChannel to) {
        new Thread(new Runnable() {
            @Override
            public void run () {
                while (true) {
                    TestTools.exchangeMessagesDuplex(from, to);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }

}