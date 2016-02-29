package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentProcessorImpl;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.etc.*;
import network.thunder.core.mesh.NodeClient;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.ECKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class LNPaymentRoutingTest {

    EmbeddedChannel channel12;
    EmbeddedChannel channel21;
    EmbeddedChannel channel23;
    EmbeddedChannel channel32;

    NodeServer node1 = new NodeServer();
    NodeServer node2 = new NodeServer();
    NodeServer node3 = new NodeServer();

    NodeClient node12 = new NodeClient(node1);
    NodeClient node21 = new NodeClient(node2);
    NodeClient node23 = new NodeClient(node2);
    NodeClient node32 = new NodeClient(node3);

    LNPaymentDBHandlerMock dbHandler1 = new LNPaymentDBHandlerMock();
    LNPaymentDBHandlerMock dbHandler2 = new LNPaymentDBHandlerMock();
    LNPaymentDBHandlerMock dbHandler3 = new LNPaymentDBHandlerMock();

    ContextFactory contextFactory1 = new MockLNPaymentContextFactory(node1, dbHandler1);
    ContextFactory contextFactory2 = new MockLNPaymentContextFactory(node2, dbHandler2);
    ContextFactory contextFactory3 = new MockLNPaymentContextFactory(node3, dbHandler3);

    LNPaymentProcessorImpl processor12;
    LNPaymentProcessorImpl processor21;
    LNPaymentProcessorImpl processor23;
    LNPaymentProcessorImpl processor32;



    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        node12.name = "LNPayment12";
        node21.name = "LNPayment21";
        node23.name = "LNPayment23";
        node32.name = "LNPayment32";

        node12.pubKeyClient = node2.pubKeyServer;
        node21.pubKeyClient = node1.pubKeyServer;
        node23.pubKeyClient = node3.pubKeyServer;
        node32.pubKeyClient = node2.pubKeyServer;

        processor12 = new LNPaymentProcessorImpl(contextFactory1, dbHandler1, node12);
        processor21 = new LNPaymentProcessorImpl(contextFactory2, dbHandler2, node21);
        processor23 = new LNPaymentProcessorImpl(contextFactory2, dbHandler2, node23);
        processor32 = new LNPaymentProcessorImpl(contextFactory3, dbHandler3, node32);

        channel12 = new EmbeddedChannel(new ProcessorHandler(processor12, "LNPayment12"));
        channel21 = new EmbeddedChannel(new ProcessorHandler(processor21, "LNPayment21"));
        channel23 = new EmbeddedChannel(new ProcessorHandler(processor23, "LNPayment23"));
        channel32 = new EmbeddedChannel(new ProcessorHandler(processor32, "LNPayment32"));

        Message m = (Message) channel21.readOutbound();
        assertNull(m);

    }

    @After
    public void after () {
        channel12.checkException();
        channel21.checkException();

        channel21.checkException();
        channel23.checkException();

        channel32.checkException();
    }

    @Test
    public void exchangePaymentWithRouting () throws InterruptedException {
        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper());
        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);
        dbHandler3.addPaymentSecret(paymentData.secret);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(3000);

        ChannelStatus status12 = processor12.getStatusTemp();
        ChannelStatus status21 = processor21.getStatusTemp();
        ChannelStatus status23 = processor23.getStatusTemp();
        ChannelStatus status32 = processor21.getStatusTemp();

        assertEquals(status12.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + paymentData.amount);
        assertEquals(status12.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL - paymentData.amount);

        assertEquals(status21.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL - paymentData.amount);
        assertEquals(status21.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + paymentData.amount);

        assertEquals(status23.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + paymentData.amount);
        assertEquals(status23.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL - paymentData.amount);

        assertEquals(status32.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL - paymentData.amount);
        assertEquals(status32.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + paymentData.amount);
    }

    @Test
    public void exchangePaymentWithRoutingRefund () throws InterruptedException {
        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper());
        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(3000);
        testUnchangedChannelAmounts();
    }

    @Test
    public void exchangePaymentWithRoutingCorruptedOnionObject () throws InterruptedException {
        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper());

        Tools.copyRandomByteInByteArray(onionObject.data, 100, 2);

        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(3000);
        testUnchangedChannelAmounts();
    }

    @Test
    public void exchangePaymentWithRoutingToUnknownNode () throws InterruptedException {
        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper(), node12.pubKeyClient.getPubKey(), new ECKey().getPubKey());

        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(3000);
        testUnchangedChannelAmounts();
    }

    @Test
    public void exchangePaymentWithTooHighPayment () throws InterruptedException {
        long normalAmount = LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL;
        long doubleAmount = normalAmount * 2;
        ChannelStatus status12 = processor12.getChannel().channelStatus;
        status12.amountClient *= 2;
        status12.amountServer *= 2;

        ChannelStatus status21 = processor21.getChannel().channelStatus;
        status21.amountClient *= 2;
        status21.amountServer *= 2;

        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper());

        PaymentData paymentData = getMockPaymentData();
        paymentData.amount = LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + 1;
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(3000);

        testUnchangedChannelAmounts(doubleAmount, doubleAmount, normalAmount, normalAmount);
    }

    public void testUnchangedChannelAmounts () {
        testUnchangedChannelAmounts(LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL,
                LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);
    }

    public void testUnchangedChannelAmounts (long amount12, long amount21, long amount23, long amount32) {
        ChannelStatus status12 = processor12.getStatusTemp();
        ChannelStatus status21 = processor21.getStatusTemp();
        ChannelStatus status23 = processor23.getStatusTemp();
        ChannelStatus status32 = processor32.getStatusTemp();

        assertEquals(status12.amountClient, amount12);
        assertEquals(status12.amountServer, amount12);

        assertEquals(status21.amountClient, amount21);
        assertEquals(status21.amountServer, amount21);

        assertEquals(status23.amountClient, amount23);
        assertEquals(status23.amountServer, amount23);

        assertEquals(status32.amountClient, amount32);
        assertEquals(status32.amountServer, amount32);
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to) {
        Object message = from.readOutbound();
        if (message != null) {
            to.writeInbound(message);
        }
    }

    public static void exchangeMessagesDuplex (EmbeddedChannel from, EmbeddedChannel to) {
        exchangeMessages(from, to);
        exchangeMessages(to, from);
    }

    public OnionObject getOnionObject (LNOnionHelper onionHelper) {
        return getOnionObject(onionHelper, node12.pubKeyClient.getPubKey(), node23.pubKeyClient.getPubKey());
    }

    public OnionObject getOnionObject (LNOnionHelper onionHelper, byte[] node2, byte[] node3) {
        List<byte[]> route = new ArrayList<>();
        route.add(node2);
        route.add(node3);
        return onionHelper.createOnionObject(route, null);
    }

    public PaymentData getMockPaymentData () {
        PaymentData paymentData = new PaymentData();
        paymentData.sending = true;
        paymentData.amount = 10000;
        paymentData.secret = new PaymentSecret(Tools.getRandomByte(20));

        return paymentData;
    }

    public void connectChannel (EmbeddedChannel from, EmbeddedChannel to) {
        new Thread(() -> {
            while (true) {
                exchangeMessagesDuplex(from, to);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    class MockLNPaymentContextFactory extends MockContextFactory {

        public MockLNPaymentContextFactory (NodeServer node, DBHandler dbHandler) {
            super(node, dbHandler);
        }

        @Override
        public LNPaymentLogic getLNPaymentLogic () {
            return new MockLNPaymentLogic();
        }

        @Override
        public LNPaymentMessageFactory getLNPaymentMessageFactory () {
            return new LNPaymentMessageFactoryMock();
        }
    }

}