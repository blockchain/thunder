package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.LNOnionHelperImpl;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentProcessorImpl;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.*;
import network.thunder.core.mesh.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class LNPaymentHandlerTest {

    EmbeddedChannel channel12;
    EmbeddedChannel channel21;


    Node node12;
    Node node21;

    LNPaymentMessageFactory messageFactory1;
    LNPaymentMessageFactory messageFactory2;

    LNPaymentProcessorImpl processor12;
    LNPaymentProcessorImpl processor21;

    MockLNPaymentLogic paymentLogic1 = new MockLNPaymentLogic();
    MockLNPaymentLogic paymentLogic2 = new MockLNPaymentLogic();

    DBHandler dbHandler1 = new LNPaymentDBHandlerMock();
    DBHandler dbHandler2 = new LNPaymentDBHandlerMock();

    LNPaymentHelper paymentHelper1;
    LNPaymentHelper paymentHelper2;

    LNOnionHelper onionHelper1;
    LNOnionHelper onionHelper2;

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        Node node1 = new Node();
        Node node2 = new Node();

        node1.isServer = false;
        node2.isServer = true;

        node12 = new Node(node1);
        node21 = new Node(node2);

        node12.name = "LNPayment12";
        node21.name = "LNPayment21";

        node12.pubKeyClient = node2.pubKeyServer;
        node21.pubKeyClient = node1.pubKeyServer;

        messageFactory1 = new LNPaymentMessageFactoryMock();
        messageFactory2 = new LNPaymentMessageFactoryMock();

        onionHelper1 = new LNOnionHelperImpl();
        onionHelper2 = new LNOnionHelperImpl();

        onionHelper1.init(node1.pubKeyServer);
        onionHelper2.init(node2.pubKeyServer);

        paymentHelper1 = new MockLNPaymentHelper();
        paymentHelper2 = new MockLNPaymentHelper();

        processor12 = new LNPaymentProcessorImpl(messageFactory1, paymentLogic1, dbHandler1, paymentHelper1, node12);
        processor21 = new LNPaymentProcessorImpl(messageFactory2, paymentLogic2, dbHandler2, paymentHelper2, node21);

        channel12 = new EmbeddedChannel(new ProcessorHandler(processor12, "LNPayment12"));
        channel21 = new EmbeddedChannel(new ProcessorHandler(processor21, "LNPayment21"));

        Message m = (Message) channel21.readOutbound();
        assertNull(m);

    }

    @After
    public void after() {
        channel12.checkException();
        channel21.checkException();
    }

    @Test
    public void fullExchangeWithNoDisturbanceWithinTimeframe () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        processor12.makePayment(getMockPaymentData());
        Thread.sleep(200);

        exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentCMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentDMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentDMessage.class);

        assertNull(channel12.readOutbound());
        assertNull(channel21.readOutbound());
    }

    @Test
    public void exchangeWithDelayShouldRestart () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        processor12.makePayment(getMockPaymentData());
        Thread.sleep(2000);

        channel21.writeInbound(channel12.readOutbound());
        channel12.writeInbound(channel21.readOutbound());
        Message message = (Message) channel12.readOutbound();

        Thread.sleep(LNPaymentProcessor.TIMEOUT_NEGOTIATION + 1000);

        channel21.writeInbound(message);
        assertNull(channel21.readOutbound());

        exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
    }

    @Test
    public void exchangeWithOtherPartyStartingOwnExchange () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        processor12.makePayment(getMockPaymentData());
        Thread.sleep(200);

        exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentCMessage.class);

        Message message = (Message) channel12.readOutbound();
        assertThat(message, instanceOf(LNPaymentDMessage.class));

        System.out.println("abort..");
        Thread.sleep(200);

        processor21.makePayment(getMockPaymentData());
        processor21.abortCurrentExchange();
        Thread.sleep(500);

        exchangeMessages(channel21, channel12, LNPaymentAMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentBMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentCMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentDMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentDMessage.class);
    }

    @Test
    public void exchangeConcurrentWithOneThrowingHigherDice () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {

        processor12.makePayment(getMockPaymentData());
        processor21.makePayment(getMockPaymentData());
        Thread.sleep(200);

        LNPaymentAMessage message1 = (LNPaymentAMessage) channel12.readOutbound();
        assertThat(message1, instanceOf(LNPaymentAMessage.class));

        LNPaymentAMessage message2 = (LNPaymentAMessage) channel21.readOutbound();
        assertThat(message2, instanceOf(LNPaymentAMessage.class));

        int dice1 = message1.dice;
        int dice2 = message2.dice;

        channel12.writeInbound(message2);
        channel21.writeInbound(message1);

        if (dice2 > dice1) {
            EmbeddedChannel channel3 = channel12;
            channel12 = channel21;
            channel21 = channel3;
        }

        exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentCMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentDMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentDMessage.class);

        Thread.sleep(200);

        exchangeMessages(channel21, channel12, LNPaymentAMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentBMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentCMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        exchangeMessages(channel21, channel12, LNPaymentDMessage.class);
        exchangeMessages(channel12, channel21, LNPaymentDMessage.class);
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