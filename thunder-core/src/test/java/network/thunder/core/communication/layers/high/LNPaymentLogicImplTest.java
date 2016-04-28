package network.thunder.core.communication.layers.high;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogicImpl;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.*;
import network.thunder.core.communication.layer.high.payments.queue.QueueElementPayment;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.LNPaymentDBHandlerMock;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Context;
import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class LNPaymentLogicImplTest {

    Channel channel1;
    Channel channel2;

    ClientObject node1;
    ClientObject node2;

    LNPaymentMessageFactory messageFactory1;
    LNPaymentMessageFactory messageFactory2;

    LNPaymentLogic paymentLogic1;
    LNPaymentLogic paymentLogic2;

    LNPaymentDBHandlerMock dbHandler1 = new LNPaymentDBHandlerMock();
    LNPaymentDBHandlerMock dbHandler2 = new LNPaymentDBHandlerMock();

    LNConfiguration configuration = new LNConfiguration();

    @Before
    public void prepare () {
        Context.getOrCreate(Constants.getNetwork());

        node1 = new ClientObject();
        node2 = new ClientObject();

        node1.isServer = false;
        node2.isServer = true;

        node1.name = "LNPayment1";
        node2.name = "LNPayment2";

        messageFactory1 = new LNPaymentMessageFactoryImpl(dbHandler1);
        messageFactory2 = new LNPaymentMessageFactoryImpl(dbHandler2);

        channel1 = new Channel();
        channel2 = new Channel();

//        channel1.channelStatus.applyConfiguration(configuration);
//        channel2.channelStatus.applyConfiguration(configuration);

        channel1.retrieveDataFromOtherChannel(channel2);
        channel2.retrieveDataFromOtherChannel(channel1);

        paymentLogic1 = new LNPaymentLogicImpl(messageFactory1, dbHandler1);
        paymentLogic2 = new LNPaymentLogicImpl(messageFactory2, dbHandler2);

        paymentLogic1.initialise(channel1);
        paymentLogic2.initialise(channel2);
    }

    @Test
    public void fullExchange () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelUpdate update = new ChannelUpdate();
        update.applyConfiguration(configuration);
        update = elementPayment.produceNewChannelStatus(channel1.channelStatus, update, null);

        LNPayment messageA = paymentLogic1.getAMessage(update);
        exchangeMessage(messageA, paymentLogic2);

        LNPayment messageB = paymentLogic2.getBMessage();
        exchangeMessage(messageB, paymentLogic1);

        LNPayment messageC1 = paymentLogic1.getCMessage();
        exchangeMessage(messageC1, paymentLogic2);

        LNPayment messageC2 = paymentLogic2.getCMessage();
        exchangeMessage(messageC2, paymentLogic1);

        LNPayment messageD1 = paymentLogic1.getDMessage();
        exchangeMessage(messageD1, paymentLogic2);

        LNPayment messageD2 = paymentLogic2.getDMessage();
        exchangeMessage(messageD2, paymentLogic1);
    }

    @Test(expected = LNPaymentException.class)
    public void partyASendsWrongSignatureOne () {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelUpdate update = new ChannelUpdate();
        update.applyConfiguration(configuration);
        update = elementPayment.produceNewChannelStatus(channel1.channelStatus, update, null);

        LNPayment messageA = paymentLogic1.getAMessage(update);
        exchangeMessage(messageA, paymentLogic2);

        LNPaymentBMessage messageB = paymentLogic2.getBMessage();
        exchangeMessage(messageB, paymentLogic1);

        LNPaymentCMessage messageC1 = paymentLogic1.getCMessage();
        messageC1.newCommitSignature1 = Tools.copyRandomByteInByteArray(messageC1.newCommitSignature1, 60, 2);
        exchangeMessage(messageC1, paymentLogic2);
    }

    @Test(expected = LNPaymentException.class)
    public void partyASendsWrongSignatureTwo () {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelUpdate update = new ChannelUpdate();
        update.applyConfiguration(configuration);
        update = elementPayment.produceNewChannelStatus(channel1.channelStatus, update, null);

        LNPayment messageA = paymentLogic1.getAMessage(update);
        exchangeMessage(messageA, paymentLogic2);

        LNPaymentBMessage messageB = paymentLogic2.getBMessage();
        exchangeMessage(messageB, paymentLogic1);

        LNPaymentCMessage messageC1 = paymentLogic1.getCMessage();
        messageC1.newCommitSignature2 = Tools.copyRandomByteInByteArray(messageC1.newCommitSignature2, 60, 2);
        exchangeMessage(messageC1, paymentLogic2);
    }

    @Test(expected = LNPaymentException.class)
    public void partyBSendsWrongSignatureOne () {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelUpdate update = new ChannelUpdate();
        update.applyConfiguration(configuration);
        update = elementPayment.produceNewChannelStatus(channel1.channelStatus, update, null);

        LNPayment messageA = paymentLogic1.getAMessage(update);
        exchangeMessage(messageA, paymentLogic2);

        LNPaymentBMessage messageB = paymentLogic2.getBMessage();
        exchangeMessage(messageB, paymentLogic1);

        LNPaymentCMessage messageC1 = paymentLogic1.getCMessage();
        exchangeMessage(messageC1, paymentLogic2);

        LNPaymentCMessage messageC2 = paymentLogic2.getCMessage();
        messageC2.newCommitSignature1 = Tools.copyRandomByteInByteArray(messageC2.newCommitSignature1, 60, 2);
        exchangeMessage(messageC1, paymentLogic1);
    }

    @Test(expected = LNPaymentException.class)
    public void partyBSendsWrongSignatureTwo () {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelUpdate update = new ChannelUpdate();
        update.applyConfiguration(configuration);
        update = elementPayment.produceNewChannelStatus(channel1.channelStatus, update, null);

        LNPayment messageA = paymentLogic1.getAMessage(update);
        exchangeMessage(messageA, paymentLogic2);

        LNPaymentBMessage messageB = paymentLogic2.getBMessage();
        exchangeMessage(messageB, paymentLogic1);

        LNPaymentCMessage messageC1 = paymentLogic1.getCMessage();
        exchangeMessage(messageC1, paymentLogic2);

        LNPaymentCMessage messageC2 = paymentLogic2.getCMessage();
        messageC2.newCommitSignature2 = Tools.copyRandomByteInByteArray(messageC2.newCommitSignature2, 60, 2);
        exchangeMessage(messageC1, paymentLogic1);
    }

    private void exchangeMessage (LNPayment message, LNPaymentLogic receiver) {
        receiver.checkMessageIncoming(message);
    }

    private PaymentData getMockPaymentData () {
        PaymentData paymentData = new PaymentData();
        paymentData.secret = new PaymentSecret(Tools.getRandomByte(20));
        paymentData.timestampOpen = Tools.currentTime();
        paymentData.timestampRefund = Tools.currentTime() + 10 * configuration.DEFAULT_REFUND_DELAY * configuration.DEFAULT_OVERLAY_REFUND;
        paymentData.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;
        return paymentData;
    }
}