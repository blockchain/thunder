package network.thunder.core.communication.layers.high;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.ProcessorHandler;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentAMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentBMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentCMessage;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentDMessage;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentMessageFactory;
import network.thunder.core.communication.layer.high.payments.LNPaymentHelper;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.LNPaymentProcessorImpl;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.high.payments.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.*;
import network.thunder.core.communication.ServerObject;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class LNPaymentHandlerTest {

    EmbeddedChannel channel12;
    EmbeddedChannel channel21;

    ServerObject serverObject1 = new ServerObject();
    ServerObject serverObject2 = new ServerObject();

    ClientObject node1 = new ClientObject(serverObject2);
    ClientObject node2 = new ClientObject(serverObject1);

    LNPaymentProcessorImpl processor12;
    LNPaymentProcessorImpl processor21;

    DBHandler dbHandler1 = new LNPaymentDBHandlerMock();
    DBHandler dbHandler2 = new LNPaymentDBHandlerMock();

    ContextFactory contextFactory12 = new MockLNPaymentContextFactory(serverObject1, dbHandler1);
    ContextFactory contextFactory21 = new MockLNPaymentContextFactory(serverObject2, dbHandler2);

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        node1.isServer = false;
        node2.isServer = true;

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
    public void fullExchangeWithNoDisturbanceWithinTimeframe () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        processor12.makePayment(getMockPaymentData());
        Thread.sleep(200);

        TestTools.exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentDMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentDMessage.class);

        assertNull(channel12.readOutbound());
        assertNull(channel21.readOutbound());

        after();
    }

    @Test
    public void exchangeWithDelayShouldRestart () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        LNPaymentProcessor.TIMEOUT_NEGOTIATION = 500;

        processor12.makePayment(getMockPaymentData());
        Thread.sleep(100);

        TestTools.exchangeMessages(channel12, channel21);
        TestTools.exchangeMessages(channel21, channel12);
        Message message = (Message) channel12.readOutbound();

        Thread.sleep((long) (LNPaymentProcessor.TIMEOUT_NEGOTIATION * 1.5));

        System.out.println(message);

        channel21.writeInbound(message);
        assertNull(channel21.readOutbound());

        TestTools.exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentBMessage.class);

        after();
    }

    @Test
    public void exchangeWithOtherPartyStartingOwnExchange () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        processor12.makePayment(getMockPaymentData());
        Thread.sleep(200);

        TestTools.exchangeMessages(channel12, channel21, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentCMessage.class);

        Message message = (Message) channel12.readOutbound();
        assertThat(message, instanceOf(LNPaymentDMessage.class));

        System.out.println("abort..");
        Thread.sleep(200);

        processor21.makePayment(getMockPaymentData());
        processor21.abortCurrentExchange();
        Thread.sleep(500);

        TestTools.exchangeMessages(channel21, channel12, LNPaymentAMessage.class);
        //Other node should ignore this new exchange
        assertNull(channel12.readOutbound());

        after();
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

        TestTools.exchangeMessages(channel21, channel12, LNPaymentBMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentDMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentDMessage.class);

        Thread.sleep(200);

        TestTools.exchangeMessages(channel21, channel12, LNPaymentAMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentBMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentCMessage.class);
        TestTools.exchangeMessages(channel21, channel12, LNPaymentDMessage.class);
        TestTools.exchangeMessages(channel12, channel21, LNPaymentDMessage.class);

        after();
    }

    public void exchangePayment (EmbeddedChannel from, EmbeddedChannel to) {
        TestTools.exchangeMessages(from, to, LNPaymentAMessage.class);
        TestTools.exchangeMessages(to, from, LNPaymentBMessage.class);
        TestTools.exchangeMessages(from, to, LNPaymentCMessage.class);
        TestTools.exchangeMessages(to, from, LNPaymentCMessage.class);
        TestTools.exchangeMessages(from, to, LNPaymentDMessage.class);
        TestTools.exchangeMessages(to, from, LNPaymentDMessage.class);
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

    class MockLNPaymentContextFactory extends MockContextFactory {

        public MockLNPaymentContextFactory (ServerObject node, DBHandler dbHandler) {
            super(node, dbHandler);
        }

        @Override
        public LNPaymentLogic getLNPaymentLogic () {
            return new MockLNPaymentLogic(getLNPaymentMessageFactory());
        }

        @Override
        public LNPaymentMessageFactory getLNPaymentMessageFactory () {
            return new LNPaymentMessageFactoryMock();
        }

        @Override
        public LNPaymentHelper getPaymentHelper () {
            return new MockLNPaymentHelper();
        }
    }

}