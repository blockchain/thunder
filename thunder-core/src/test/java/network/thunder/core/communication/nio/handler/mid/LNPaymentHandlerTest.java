package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.nio.handler.ProcessorHandler;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentProcessorImpl;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.etc.LNPaymentDBHandlerMock;
import network.thunder.core.etc.LNPaymentMessageFactoryMock;
import network.thunder.core.etc.MockLNPaymentLogic;
import network.thunder.core.etc.crypto.ECDH;
import network.thunder.core.mesh.Node;
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

    EmbeddedChannel channel1;
    EmbeddedChannel channel2;

    Node node1;
    Node node2;

    LNPaymentMessageFactory messageFactory1;
    LNPaymentMessageFactory messageFactory2;

    LNPaymentProcessor processor1;
    LNPaymentProcessor processor2;

    MockLNPaymentLogic paymentLogic1 = new MockLNPaymentLogic();
    MockLNPaymentLogic paymentLogic2 = new MockLNPaymentLogic();

    LNPaymentDBHandlerMock dbHandler1 = new LNPaymentDBHandlerMock();
    LNPaymentDBHandlerMock dbHandler2 = new LNPaymentDBHandlerMock();

    //    EncryptionHandler handler;
    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        node1 = new Node();
        node2 = new Node();

        node1.isServer = false;
        node2.isServer = true;

        node1.name = "LNPayment1";
        node2.name = "LNPayment2";

        node1.ephemeralKeyClient = node2.ephemeralKeyServer;
        node2.ephemeralKeyClient = node1.ephemeralKeyServer;

        node1.ecdhKeySet = ECDH.getSharedSecret(node1.ephemeralKeyServer, node1.ephemeralKeyClient);
        node2.ecdhKeySet = ECDH.getSharedSecret(node2.ephemeralKeyServer, node2.ephemeralKeyClient);

        messageFactory1 = new LNPaymentMessageFactoryMock();
        messageFactory2 = new LNPaymentMessageFactoryMock();

        processor1 = new LNPaymentProcessorImpl(messageFactory1, paymentLogic1, dbHandler1, node1);
        processor2 = new LNPaymentProcessorImpl(messageFactory2, paymentLogic2, dbHandler2, node2);

        channel1 = new EmbeddedChannel(new ProcessorHandler(processor1, "LNPayment1"));
        channel2 = new EmbeddedChannel(new ProcessorHandler(processor2, "LNPayment2"));

        Message m = (Message) channel2.readOutbound();
        assertNull(m);

    }

    @Test
    public void fullExchangeWithNoDisturbanceWithinTimeframe () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        processor1.makePayment(getMockPaymentData(), null);
        Thread.sleep(10);

        exchangeMessages(channel1, channel2, LNPaymentAMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentBMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentCMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentCMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentDMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentDMessage.class);

        assertNull(channel1.readOutbound());
        assertNull(channel2.readOutbound());
    }

    @Test
    public void exchangeWithDelayShouldRestart () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        processor1.makePayment(getMockPaymentData(), null);
        Thread.sleep(10);

        channel2.writeInbound(channel1.readOutbound());
        channel1.writeInbound(channel2.readOutbound());
        Message message = (Message) channel1.readOutbound();

        Thread.sleep(LNPaymentProcessor.TIMEOUT_NEGOTIATION + 1000);

        channel2.writeInbound(message);
        assertNull(channel2.readOutbound());

        exchangeMessages(channel1, channel2, LNPaymentAMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentBMessage.class);
    }

    @Test
    public void exchangeWithOtherPartyStartingOwnExchange () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        processor1.makePayment(getMockPaymentData(), null);
        Thread.sleep(10);

        exchangeMessages(channel1, channel2, LNPaymentAMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentBMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentCMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentCMessage.class);

        Message message = (Message) channel1.readOutbound();
        assertThat(message, instanceOf(LNPaymentDMessage.class));

        System.out.println("abort..");
        Thread.sleep(100);

        processor2.makePayment(getMockPaymentData(), null);
        processor2.abortCurrentExchange();
        Thread.sleep(10);

        exchangeMessages(channel2, channel1, LNPaymentAMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentBMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentCMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentCMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentDMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentDMessage.class);
    }

    @Test
    public void exchangeConcurrentWithOneThrowingHigherDice () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {

        processor1.makePayment(getMockPaymentData(), null);
        processor2.makePayment(getMockPaymentData(), null);
        Thread.sleep(10);

        LNPaymentAMessage message1 = (LNPaymentAMessage) channel1.readOutbound();
        assertThat(message1, instanceOf(LNPaymentAMessage.class));

        LNPaymentAMessage message2 = (LNPaymentAMessage) channel2.readOutbound();
        assertThat(message2, instanceOf(LNPaymentAMessage.class));

        int dice1 = message1.dice;
        int dice2 = message2.dice;

        channel1.writeInbound(message2);
        channel2.writeInbound(message1);

        if (dice2 > dice1) {
            EmbeddedChannel channel3 = channel1;
            channel1 = channel2;
            channel2 = channel3;
        }

        exchangeMessages(channel2, channel1, LNPaymentBMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentCMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentCMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentDMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentDMessage.class);

        Thread.sleep(100);

        exchangeMessages(channel2, channel1, LNPaymentAMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentBMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentCMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentCMessage.class);
        exchangeMessages(channel2, channel1, LNPaymentDMessage.class);
        exchangeMessages(channel1, channel2, LNPaymentDMessage.class);
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to) {
        Object message = from.readOutbound();
        if (message != null) {
            to.writeInbound(message);
        } else {
            System.out.println("Null message..");
        }
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to, Class expectedMessage) {
        Object message = from.readOutbound();
        assertThat(message, instanceOf(expectedMessage));
        if (message != null) {
            to.writeInbound(message);
        } else {
            System.out.println("Null message..");
        }
    }

    public static void exchangeMessagesDuplex (EmbeddedChannel from, EmbeddedChannel to) {
        exchangeMessages(from, to);
        exchangeMessages(to, from);
    }

    public PaymentData getMockPaymentData () {
        return new PaymentData();
    }

}