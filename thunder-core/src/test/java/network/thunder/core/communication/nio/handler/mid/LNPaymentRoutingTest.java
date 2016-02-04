package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.LNOnionHelperImpl;
import network.thunder.core.communication.objects.messages.impl.LNPaymentHelperImpl;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.*;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentProcessorImpl;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.etc.LNPaymentDBHandlerMock;
import network.thunder.core.etc.LNPaymentMessageFactoryMock;
import network.thunder.core.etc.MockLNPaymentLogic;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.Node;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class LNPaymentRoutingTest {

    EmbeddedChannel channel12;

    EmbeddedChannel channel21;
    EmbeddedChannel channel23;

    EmbeddedChannel channel32;

    Node node12;

    Node node21;
    Node node23;

    Node node32;

    LNPaymentMessageFactory messageFactory1;
    LNPaymentMessageFactory messageFactory2;
    LNPaymentMessageFactory messageFactory3;

    LNPaymentProcessorImpl processor12;

    LNPaymentProcessorImpl processor21;
    LNPaymentProcessorImpl processor23;

    LNPaymentProcessorImpl processor32;

    MockLNPaymentLogic paymentLogic1 = new MockLNPaymentLogic();
    MockLNPaymentLogic paymentLogic2 = new MockLNPaymentLogic();
    MockLNPaymentLogic paymentLogic3 = new MockLNPaymentLogic();

    LNPaymentDBHandlerMock dbHandler1 = new LNPaymentDBHandlerMock();
    LNPaymentDBHandlerMock dbHandler2 = new LNPaymentDBHandlerMock();
    LNPaymentDBHandlerMock dbHandler3 = new LNPaymentDBHandlerMock();

    LNPaymentHelper paymentHelper1;
    LNPaymentHelper paymentHelper2;
    LNPaymentHelper paymentHelper3;

    LNOnionHelper onionHelper1;
    LNOnionHelper onionHelper2;
    LNOnionHelper onionHelper3;

    //    EncryptionHandler handler;
    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        Node node1 = new Node();
        Node node2 = new Node();
        Node node3 = new Node();

        node1.isServer = false;
        node2.isServer = true;
        node3.isServer = false;

        node12 = new Node(node1);
        node21 = new Node(node2);
        node23 = new Node(node2);
        node32 = new Node(node3);

        node12.name = "LNPayment12";
        node21.name = "LNPayment21";
        node23.name = "LNPayment23";
        node32.name = "LNPayment32";

        node12.pubKeyClient = node2.pubKeyServer;
        node21.pubKeyClient = node1.pubKeyServer;
        node23.pubKeyClient = node3.pubKeyServer;
        node32.pubKeyClient = node2.pubKeyServer;

        messageFactory1 = new LNPaymentMessageFactoryMock();
        messageFactory2 = new LNPaymentMessageFactoryMock();
        messageFactory3 = new LNPaymentMessageFactoryMock();

        onionHelper1 = new LNOnionHelperImpl();
        onionHelper2 = new LNOnionHelperImpl();
        onionHelper3 = new LNOnionHelperImpl();

        onionHelper1.init(node1.pubKeyServer);
        onionHelper2.init(node2.pubKeyServer);
        onionHelper3.init(node3.pubKeyServer);

        paymentHelper1 = new LNPaymentHelperImpl(onionHelper1, dbHandler1);
        paymentHelper2 = new LNPaymentHelperImpl(onionHelper2, dbHandler2);
        paymentHelper3 = new LNPaymentHelperImpl(onionHelper3, dbHandler3);

        processor12 = new LNPaymentProcessorImpl(messageFactory1, paymentLogic1, dbHandler1, paymentHelper1, node12);

        processor21 = new LNPaymentProcessorImpl(messageFactory2, paymentLogic2, dbHandler2, paymentHelper2, node21);
        processor23 = new LNPaymentProcessorImpl(messageFactory2, paymentLogic2, dbHandler2, paymentHelper2, node23);

        processor32 = new LNPaymentProcessorImpl(messageFactory3, paymentLogic3, dbHandler3, paymentHelper3, node32);

        channel12 = new EmbeddedChannel(new ProcessorHandler(processor12, "LNPayment12"));

        channel21 = new EmbeddedChannel(new ProcessorHandler(processor21, "LNPayment21"));
        channel23 = new EmbeddedChannel(new ProcessorHandler(processor23, "LNPayment23"));

        channel32 = new EmbeddedChannel(new ProcessorHandler(processor32, "LNPayment32"));

        Message m = (Message) channel21.readOutbound();
        assertNull(m);

    }

    @Test
    public void exchangePaymentWithRouting () throws InterruptedException {
        OnionObject onionObject = getOnionObject(onionHelper1);
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

        ChannelStatus status12 = processor12.getChannelStatus();
        ChannelStatus status21 = processor21.getChannelStatus();
        ChannelStatus status23 = processor23.getChannelStatus();
        ChannelStatus status32 = processor21.getChannelStatus();

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
        OnionObject onionObject = getOnionObject(onionHelper1);
        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(3000);

        ChannelStatus status12 = processor12.getChannelStatus();
        ChannelStatus status21 = processor21.getChannelStatus();
        ChannelStatus status23 = processor23.getChannelStatus();
        ChannelStatus status32 = processor21.getChannelStatus();

        assertEquals(status12.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);
        assertEquals(status12.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);

        assertEquals(status21.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);
        assertEquals(status21.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);

        assertEquals(status23.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);
        assertEquals(status23.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);

        assertEquals(status32.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);
        assertEquals(status32.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);

    }

    public void exchangePayment (EmbeddedChannel from, EmbeddedChannel to) {
        exchangeMessages(from, to, LNPaymentAMessage.class);
        exchangeMessages(to, from, LNPaymentBMessage.class);
        exchangeMessages(from, to, LNPaymentCMessage.class);
        exchangeMessages(to, from, LNPaymentCMessage.class);
        exchangeMessages(from, to, LNPaymentDMessage.class);
        exchangeMessages(to, from, LNPaymentDMessage.class);
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to) {
        Object message = from.readOutbound();
        if (message != null) {
            to.writeInbound(message);
        }
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to, Class expectedMessage) {
        Object message = from.readOutbound();
        assertThat(message, instanceOf(expectedMessage));
        if (message != null) {
            to.writeInbound(message);
        }
    }

    public static void exchangeMessagesDuplex (EmbeddedChannel from, EmbeddedChannel to) {
        exchangeMessages(from, to);
        exchangeMessages(to, from);
    }

    public OnionObject getOnionObject (LNOnionHelper onionHelper) {
        List<byte[]> route = new ArrayList<>();
        route.add(node21.pubKeyServer.getPubKey());
        route.add(node32.pubKeyServer.getPubKey());
        return onionHelper.createOnionObject(route);
    }

    public PaymentData getMockPaymentData () {
        PaymentData paymentData = new PaymentData();
        paymentData.sending = true;
        paymentData.amount = 10000;
        paymentData.secret = new PaymentSecret(Tools.getRandomByte(20));

        return paymentData;
    }

    public void connectChannel (EmbeddedChannel from, EmbeddedChannel to) {
        new Thread(new Runnable() {
            @Override
            public void run () {
                while (true) {
                    exchangeMessagesDuplex(from, to);
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