package network.thunder.core.communication.processor.implementations.lnpayment;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNOnionHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.communication.processor.implementations.lnpayment.helper.*;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.mesh.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentProcessorImpl.Status.*;
import static network.thunder.core.database.objects.PaymentStatus.EMBEDDED;
import static network.thunder.core.database.objects.PaymentStatus.REFUNDED;

/**
 * Created by matsjerratsch on 04/01/2016.
 */
public class LNPaymentProcessorImpl extends LNPaymentProcessor {
    LNPaymentMessageFactory messageFactory;
    LNPaymentLogic paymentLogic;
    DBHandler dbHandler;
    LNPaymentHelper paymentHelper;
    Node node;

    MessageExecutor messageExecutor;
    LNOnionHelper onionHelper;

    Channel channel;

    Status status = IDLE;

    LinkedBlockingDeque<QueueElement> queueList = new LinkedBlockingDeque<>(1000);
    List<QueueElement> currentQueueElement = new ArrayList<>();

    ChannelStatus statusTemp;
    boolean aborted = false;
    boolean finished = false;

    boolean weStartedExchange = false;

    CountDownLatch countDownLatch = new CountDownLatch(1);
    long currentTaskStarted;

    int latestDice = 0;

    public LNPaymentProcessorImpl (ContextFactory contextFactory, DBHandler dbHandler, NodeClient node) {
        this.messageFactory = contextFactory.getLNPaymentMessageFactory();
        this.paymentLogic = contextFactory.getLNPaymentLogic();
        this.dbHandler = dbHandler;
        this.paymentHelper = contextFactory.getPaymentHelper();
        this.eventHelper = contextFactory.getEventHelper();
        this.node = node;
    }

    private void startQueueListener () {
        new Thread(() -> {
            while (true) {
                try {
                    checkQueue();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void checkQueue () throws InterruptedException {
        if (status == IDLE) {
            QueueElement element = queueList.poll(100, TimeUnit.MILLISECONDS);
            if (element != null) {
                if (status == IDLE) {
                    currentQueueElement.add(element);
                    while (queueList.size() > 0) {
                        currentQueueElement.add(queueList.poll());
                    }

                    buildChannelStatus();
                    if (!checkForUpdates()) {
                        return;
                    }

                    sendMessageA();
                    restartCountDown(TIMEOUT_NEGOTIATION);

                    if (weStartedExchange && status != IDLE) {
                        //Time is over - we try it again after some random delay?
                        putQueueElementsBackInQueue();
                        setStatus(IDLE);
                    }

                } else {
                    queueList.addFirst(element);
                }
            }

        } else {
            //When we get here and Status is not IDLE, the other party started the exchange..
            long timeToFinish = TIMEOUT_NEGOTIATION - (System.currentTimeMillis() - currentTaskStarted);

            restartCountDown(timeToFinish);
            if (status != IDLE) {
                //Other party started the exchange, but we hit the timeout for negotiation. Just abort it on our side..
                statusTemp = channel.channelStatus;
                setStatus(IDLE);
            }
        }
    }

    private void buildChannelStatus () {
        ChannelStatus temp = channel.channelStatus.getClone();
        System.out.println(node.name + " :" + temp);
        for (QueueElement queueElement : currentQueueElement) {
            temp = queueElement.produceNewChannelStatus(temp, paymentHelper);
        }
        System.out.println(node.name + " :" + temp);
        this.statusTemp = temp;
    }

    private boolean checkForUpdates () {
        int totalChanges = statusTemp.newPayments.size() + statusTemp.redeemedPayments.size() + statusTemp.refundedPayments.size();
        return totalChanges > 0;
    }

    private void putQueueElementsBackInQueue () {
        for (int i = currentQueueElement.size() - 1; i >= 0; --i) {
            queueList.addFirst(currentQueueElement.get(i));
        }
        currentQueueElement.clear();
    }

    public void restartCountDown (long sleepTime) throws InterruptedException {
        System.out.println(node.name + " START TIME " + sleepTime);

        countDownLatch = new CountDownLatch(1);
        countDownLatch.await(sleepTime, TimeUnit.MILLISECONDS);
        System.out.println(node.name + " STOP TIME");

    }

    public void abortCountDown () {
        countDownLatch.countDown();
    }

    @Override
    public boolean makePayment (PaymentData paymentData) {
        QueueElement payment = new QueueElementPayment(paymentData);
        queueList.add(payment);
        return true;
    }

    @Override
    public boolean redeemPayment (PaymentSecret paymentSecret) {
        QueueElementRedeem payment = new QueueElementRedeem(paymentSecret);
        queueList.add(payment);
        return true;
    }

    @Override
    public boolean refundPayment (PaymentData paymentData) {
        QueueElementRefund payment = new QueueElementRefund(paymentData.secret);
        queueList.add(payment);
        return true;
    }

    public void abortCurrentExchange () {
        System.out.println(node.name + " ABORT");
        abortCountDown();
    }

    private void sendMessageA () {
        testStatus(IDLE);
        weStartedExchange = true;

        LNPaymentAMessage message = messageFactory.getMessageA(channel, statusTemp);
        paymentLogic.readMessageOutbound(message);
        latestDice = message.dice;
        sendMessage(message);

        setStatus(SENT_A);
    }

    private void sendMessageB () {
        testStatus(RECEIVED_A);

        LNPaymentBMessage message = messageFactory.getMessageB(channel);
        paymentLogic.readMessageOutbound(message);
        sendMessage(message);

        setStatus(SENT_B);

    }

    private void sendMessageC () {
        if (weStartedExchange) {
            testStatus(RECEIVED_B);
        } else {
            testStatus(RECEIVED_C);
        }

        //TODO sending and constructing message..
        LNPayment message = messageFactory.getMessageC(channel, paymentLogic.getClientTransaction());
        paymentLogic.readMessageOutbound(message);
        sendMessage(message);

        setStatus(SENT_C);
    }

    private void sendMessageD () {
        if (weStartedExchange) {
            testStatus(RECEIVED_C);
        } else {
            testStatus(RECEIVED_D);
        }

        //TODO sending and constructing message..
        LNPayment message = messageFactory.getMessageD(channel);
        paymentLogic.readMessageOutbound(message);
        sendMessage(message);

        setStatus(SENT_D);

        if (!weStartedExchange) {
            finishCurrentTask();
        }

    }

    private void readMessageA (LNPaymentAMessage message) {
        if (status == SENT_A) {
            if (message.dice > latestDice) {
                System.out.println(node.name + " DICE HIGHER...");
                weStartedExchange = false;
                currentTaskStarted = System.currentTimeMillis();
                abortCurrentTask();
            } else {
                System.out.println(node.name + " Ignoring because we had higher dice..");
                return;
            }
        } else if (status != IDLE) {
            System.out.println(node.name + " WE ABORT BECAUSE OTHER PARTY SENT US A NEW REQUEST..");
            abortCurrentTask();
        }

        weStartedExchange = false;
        currentTaskStarted = System.currentTimeMillis();

        paymentLogic.checkMessageIncoming(message);

        setStatus(RECEIVED_A);
        sendMessageB();
    }

    private void readMessageB (LNPaymentBMessage message) {
        testStatus(SENT_A);

        paymentLogic.checkMessageIncoming(message);
        setStatus(RECEIVED_B);
        sendMessageC();

    }

    private void readMessageC (LNPaymentCMessage message) {
        if ((weStartedExchange && status != SENT_C) || (!weStartedExchange && status != SENT_B)) {
            //TODO ERROR
            System.out.println(node.name + " ERROR READ MESSAGE C");

        } else {
            paymentLogic.checkMessageIncoming(message);
            setStatus(RECEIVED_C);
            if (weStartedExchange) {
                sendMessageD();
            } else {
                sendMessageC();
            }
        }
    }

    private void readMessageD (LNPaymentDMessage message) {
        if ((weStartedExchange && status != SENT_D) || (!weStartedExchange && status != SENT_C)) {
            //TODO ERROR
            System.out.println(node.name + " ERROR READ MESSAGE D");
            return;
        } else {
            paymentLogic.checkMessageIncoming(message);
            setStatus(RECEIVED_D);
        }
        if (weStartedExchange) {
            finishCurrentTask();
        } else {
            sendMessageD();
        }
    }

    public void testStatus (Status expected) {
        if (status != expected) {
            throw new RuntimeException("Expected " + expected + ". Was: " + status);
        }
    }

    private void abortCurrentTask () {
        aborted = true;
        putQueueElementsBackInQueue();

        //TODO
        weStartedExchange = false;
        currentQueueElement.add(new QueueElementUpdate());

        System.out.println(node.name + " abortCurrentTask");

        abortCountDown();
    }

    private void finishCurrentTask () {
        updateDatabase();
        evaluateUpdates();

        finished = true;
        aborted = false;
        setStatus(IDLE);
        System.out.println(node.name + " finishCurrentTask");
        abortCountDown();
    }

    private void updateDatabase () {
        ChannelStatus status = paymentLogic.getTemporaryChannelStatus();
        for (PaymentData payment : status.newPayments) {
            if (weStartedExchange) {
                PaymentWrapper wrapper = dbHandler.getPayment(payment.secret);
                if (wrapper == null) {
                    wrapper = new PaymentWrapper(new byte[0], payment);
                    wrapper.statusReceiver = EMBEDDED;
                    dbHandler.addPayment(wrapper);
                } else {
                    wrapper.statusReceiver = EMBEDDED;
                    dbHandler.updatePaymentReceiver(wrapper);
                }
            } else {
                PaymentWrapper wrapper = new PaymentWrapper(node.pubKeyClient.getPubKey(), payment);
                System.out.println(node.name + " addPayment: " + payment);
                dbHandler.addPayment(wrapper);
            }
        }
        for (PaymentData payment : status.refundedPayments) {
            PaymentWrapper wrapper = dbHandler.getPayment(payment.secret);
            if (weStartedExchange) {
                wrapper.statusReceiver = REFUNDED;
                dbHandler.updatePaymentReceiver(wrapper);
            } else {
                wrapper.statusSender = REFUNDED;
                dbHandler.updatePaymentSender(wrapper);
            }
        }
        for (PaymentData payment : status.redeemedPayments) {
            PaymentWrapper wrapper = dbHandler.getPayment(payment.secret);
            if (weStartedExchange) {
                wrapper.statusReceiver = REFUNDED;
                dbHandler.updatePaymentReceiver(wrapper);
            } else {
                wrapper.statusSender = REFUNDED;
                dbHandler.updatePaymentSender(wrapper);
            }
        }

    }

    private void evaluateUpdates () {
        statusTemp = paymentLogic.getTemporaryChannelStatus();

        System.out.println(node.name + " " + statusTemp);

        ChannelStatus statusToBeProcessed = statusTemp.getClone();

        statusTemp.oldPayments.addAll(statusTemp.newPayments);
        statusTemp.newPayments.clear();
        statusTemp.refundedPayments.clear();
        statusTemp.redeemedPayments.clear();
        channel.channelStatus = statusTemp;

        if (!weStartedExchange) {
            for (PaymentData newPayment : statusToBeProcessed.newPayments) {
                paymentHelper.relayPayment(this, newPayment);
            }

            for (PaymentData redeemedPayment : statusToBeProcessed.redeemedPayments) {
                paymentHelper.paymentRedeemed(redeemedPayment.secret);
            }

            for (PaymentData refundedPayment : statusToBeProcessed.refundedPayments) {
                paymentHelper.paymentRefunded(refundedPayment);
            }
        }

    }

    private void sendMessage (Message message) {
        messageExecutor.sendMessageUpwards(message);
    }

    private void setStatus (Status status) {
        System.out.println(node.name + " SET STATUS FROM " + this.status + " TO " + status);
        this.status = status;
    }

    @Override
    public void onInboundMessage (Message message) {
        if (message instanceof LNPayment) {
            if (message instanceof LNPaymentAMessage) {
                readMessageA((LNPaymentAMessage) message);
            } else if (message instanceof LNPaymentBMessage) {
                readMessageB((LNPaymentBMessage) message);
            } else if (message instanceof LNPaymentCMessage) {
                readMessageC((LNPaymentCMessage) message);
            } else if (message instanceof LNPaymentDMessage) {
                readMessageD((LNPaymentDMessage) message);
            }
        }
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
        this.paymentHelper.addProcessor(this);
        startQueueListener();
    }

    @Override
    public void onLayerClose () {
        this.paymentHelper.removeProcessor(this);
    }

    @Override
    public boolean consumesInboundMessage (Object object) {
        return (object instanceof LNPayment);
    }

    @Override
    public boolean consumesOutboundMessage (Object object) {
        return false;
    }

    public ChannelStatus getStatusTemp () {
        if (statusTemp == null) {
            statusTemp = channel.channelStatus;
        }
        return statusTemp;
    }

    public Channel getChannel () {
        return channel;
    }

    @Override
    public boolean connectsToNodeId (byte[] nodeId) {
        return Arrays.equals(nodeId, node.pubKeyClient.getPubKey());
    }

    @Override
    public byte[] connectsTo () {
        return node.pubKeyClient.getPubKey();
    }

    public enum Status {
        IDLE,
        SENT_A,
        RECEIVED_A,
        SENT_B,
        RECEIVED_B,
        SENT_C,
        RECEIVED_C,
        SENT_D,
        RECEIVED_D
    }

}
