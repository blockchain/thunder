package network.thunder.core.communication.processor.implementations.lnpayment;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.factories.LNPaymentMessageFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.communication.processor.implementations.lnpayment.helper.QueueElementPayment;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.LNPaymentDBHandlerMock;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.LNConfiguration;
import network.thunder.core.mesh.NodeClient;
import org.bitcoinj.core.Context;
import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by matsjerratsch on 02/11/2015.
 */
public class LNPaymentLogicImplTest {

    Channel channel1;
    Channel channel2;

    NodeClient node1;
    NodeClient node2;

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

        node1 = new NodeClient();
        node2 = new NodeClient();

        node1.isServer = false;
        node2.isServer = true;

        node1.name = "LNPayment1";
        node2.name = "LNPayment2";

        messageFactory1 = new LNPaymentMessageFactoryImpl(dbHandler1);
        messageFactory2 = new LNPaymentMessageFactoryImpl(dbHandler2);

        channel1 = new Channel();
        channel2 = new Channel();

        channel1.channelStatus.applyConfiguration(configuration);
        channel2.channelStatus.applyConfiguration(configuration);

        channel1.retrieveDataFromOtherChannel(channel2);
        channel2.retrieveDataFromOtherChannel(channel1);

        paymentLogic1 = new LNPaymentLogicImpl(dbHandler1);
        paymentLogic2 = new LNPaymentLogicImpl(dbHandler2);

        paymentLogic1.initialise(channel1);
        paymentLogic2.initialise(channel2);
    }


    @Test
    public void fullExchange () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelStatus status1 = elementPayment.produceNewChannelStatus(channel1.channelStatus, null);

        LNPayment messageA = messageFactory1.getMessageA(channel1, status1);
        exchangeMessage(messageA, paymentLogic1, paymentLogic2);

        LNPayment messageB = messageFactory2.getMessageB(channel2);
        exchangeMessage(messageB, paymentLogic2, paymentLogic1);

        LNPayment messageC1 = messageFactory1.getMessageC(channel1, paymentLogic1.getChannelSignatures(), paymentLogic1.getPaymentSignatures());
        exchangeMessage(messageC1, paymentLogic1, paymentLogic2);

        LNPayment messageC2 = messageFactory2.getMessageC(channel2, paymentLogic2.getChannelSignatures(), paymentLogic2.getPaymentSignatures());
        exchangeMessage(messageC2, paymentLogic2, paymentLogic1);

        LNPayment messageD1 = messageFactory1.getMessageD(channel1);
        exchangeMessage(messageD1, paymentLogic1, paymentLogic2);

        LNPayment messageD2 = messageFactory2.getMessageD(channel2);
        exchangeMessage(messageD2, paymentLogic2, paymentLogic1);
    }

    @Test(expected = LNPaymentException.class)
    public void sentSuccessFalse () throws NoSuchProviderException, NoSuchAlgorithmException, InterruptedException {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelStatus status1 = elementPayment.produceNewChannelStatus(channel1.channelStatus, null);

        LNPayment messageA = messageFactory1.getMessageA(channel1, status1);
        exchangeMessage(messageA, paymentLogic1, paymentLogic2);

        LNPaymentBMessage messageB = messageFactory2.getMessageB(channel2);
        messageB.success = false;
        exchangeMessage(messageB, paymentLogic2, paymentLogic1);
    }

    @Test(expected = LNPaymentException.class)
    public void partyASendsWrongSignatureOne () {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelStatus status1 = elementPayment.produceNewChannelStatus(channel1.channelStatus, null);

        LNPayment messageA = messageFactory1.getMessageA(channel1, status1);
        exchangeMessage(messageA, paymentLogic1, paymentLogic2);

        LNPayment messageB = messageFactory2.getMessageB(channel2);
        exchangeMessage(messageB, paymentLogic2, paymentLogic1);

        LNPaymentCMessage messageC1 = messageFactory1.getMessageC(channel1, paymentLogic1.getChannelSignatures(), paymentLogic1.getPaymentSignatures());
        messageC1.newCommitSignature1 = Tools.copyRandomByteInByteArray(messageC1.newCommitSignature1, 60, 2);
        exchangeMessage(messageC1, paymentLogic1, paymentLogic2);
    }

    @Test(expected = LNPaymentException.class)
    public void partyASendsWrongSignatureTwo () {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelStatus status1 = elementPayment.produceNewChannelStatus(channel1.channelStatus, null);

        LNPayment messageA = messageFactory1.getMessageA(channel1, status1);
        exchangeMessage(messageA, paymentLogic1, paymentLogic2);

        LNPayment messageB = messageFactory2.getMessageB(channel2);
        exchangeMessage(messageB, paymentLogic2, paymentLogic1);

        LNPaymentCMessage messageC1 = messageFactory1.getMessageC(channel1, paymentLogic1.getChannelSignatures(), paymentLogic1.getPaymentSignatures());
        messageC1.newCommitSignature2 = Tools.copyRandomByteInByteArray(messageC1.newCommitSignature2, 60, 2);
        exchangeMessage(messageC1, paymentLogic1, paymentLogic2);
    }

    @Test(expected = LNPaymentException.class)
    public void partyBSendsWrongSignatureOne () {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelStatus status1 = elementPayment.produceNewChannelStatus(channel1.channelStatus, null);

        LNPayment messageA = messageFactory1.getMessageA(channel1, status1);
        exchangeMessage(messageA, paymentLogic1, paymentLogic2);

        LNPayment messageB = messageFactory2.getMessageB(channel2);
        exchangeMessage(messageB, paymentLogic2, paymentLogic1);

        LNPaymentCMessage messageC1 = messageFactory1.getMessageC(channel1, paymentLogic1.getChannelSignatures(), paymentLogic1.getPaymentSignatures());
        exchangeMessage(messageC1, paymentLogic1, paymentLogic2);

        LNPaymentCMessage messageC2 = messageFactory2.getMessageC(channel2, paymentLogic2.getChannelSignatures(), paymentLogic2.getPaymentSignatures());
        messageC2.newCommitSignature1 = Tools.copyRandomByteInByteArray(messageC2.newCommitSignature1, 60, 2);
        exchangeMessage(messageC1, paymentLogic2, paymentLogic1);
    }

    @Test(expected = LNPaymentException.class)
    public void partyBSendsWrongSignatureTwo () {
        PaymentData paymentData = getMockPaymentData();
        QueueElementPayment elementPayment = new QueueElementPayment(paymentData);

        ChannelStatus status1 = elementPayment.produceNewChannelStatus(channel1.channelStatus, null);

        LNPayment messageA = messageFactory1.getMessageA(channel1, status1);
        exchangeMessage(messageA, paymentLogic1, paymentLogic2);

        LNPayment messageB = messageFactory2.getMessageB(channel2);
        exchangeMessage(messageB, paymentLogic2, paymentLogic1);

        LNPaymentCMessage messageC1 = messageFactory1.getMessageC(channel1, paymentLogic1.getChannelSignatures(), paymentLogic1.getPaymentSignatures());
        exchangeMessage(messageC1, paymentLogic1, paymentLogic2);

        LNPaymentCMessage messageC2 = messageFactory2.getMessageC(channel2, paymentLogic2.getChannelSignatures(), paymentLogic2.getPaymentSignatures());
        messageC2.newCommitSignature2 = Tools.copyRandomByteInByteArray(messageC2.newCommitSignature2, 60, 2);
        exchangeMessage(messageC1, paymentLogic2, paymentLogic1);
    }

    private void exchangeMessage (LNPayment message, LNPaymentLogic sender, LNPaymentLogic receiver) {
        sender.readMessageOutbound(message);
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