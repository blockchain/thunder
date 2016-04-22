package network.thunder.core.communication.layer.high.payments;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageExecutor;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.messages.*;
import network.thunder.core.communication.layer.high.payments.queue.*;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.helper.events.LNEventHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static network.thunder.core.communication.layer.high.payments.LNPaymentProcessorImpl.Status.*;
import static network.thunder.core.database.objects.PaymentStatus.*;

//TODO this class is very stateful. Currently the channel can break if a disconnect happens between the D messages.
//      Save the state to disk after each message and be able to read it upon connection opening if we want to be able to replay lost messages..
public class LNPaymentProcessorImpl extends LNPaymentProcessor {
    LNPaymentMessageFactory messageFactory;
    LNPaymentLogic paymentLogic;
    DBHandler dbHandler;
    LNPaymentHelper paymentHelper;
    LNEventHelper eventHelper;
    ClientObject node;
    ServerObject serverObject;

    MessageExecutor messageExecutor;

    Channel channel;

    Status status = IDLE;

    BlockingDeque<QueueElement> queueList = new LinkedBlockingDeque<>(1000);
    List<QueueElement> currentQueueElement = new ArrayList<>();

    ChannelUpdate updateTemp;
    boolean aborted = false;
    boolean finished = false;

    boolean weStartedExchange = false;

    CountDownLatch countDownLatch = new CountDownLatch(1);
    long currentTaskStarted;

    int latestDice = 0;

    boolean connectionClosed = false;

    public LNPaymentProcessorImpl (ContextFactory contextFactory, DBHandler dbHandler, ClientObject node) {
        this.messageFactory = contextFactory.getLNPaymentMessageFactory();
        this.paymentLogic = contextFactory.getLNPaymentLogic();
        this.dbHandler = dbHandler;
        this.paymentHelper = contextFactory.getPaymentHelper();
        this.eventHelper = contextFactory.getEventHelper();
        this.node = node;
        this.serverObject = contextFactory.getServerSettings();
    }

    private void startQueueListener () {
        new Thread(() -> {
            while (!connectionClosed) {
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
                setStatus(IDLE);
            }
        }
    }

    private void buildChannelStatus () {
        ChannelUpdate update = new ChannelUpdate();
        update.applyConfiguration(serverObject.configuration);
        for (QueueElement queueElement : currentQueueElement) {
            update = queueElement.produceNewChannelStatus(channel.channelStatus, update, paymentHelper);
        }
        this.updateTemp = update;
    }

    private boolean checkForUpdates () {
        int totalChanges = updateTemp.newPayments.size() + updateTemp.redeemedPayments.size() + updateTemp.refundedPayments.size();
        return totalChanges > 0;
    }

    private void putQueueElementsBackInQueue () {
        for (int i = currentQueueElement.size() - 1; i >= 0; --i) {
            queueList.addFirst(currentQueueElement.get(i));
        }
        currentQueueElement.clear();
    }

    public void restartCountDown (long sleepTime) throws InterruptedException {
        countDownLatch = new CountDownLatch(1);
        countDownLatch.await(sleepTime, TimeUnit.MILLISECONDS);
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
        abortCountDown();
    }

    private void sendMessageA () {
        testStatus(IDLE);
        weStartedExchange = true;

        LNPaymentAMessage message = paymentLogic.getAMessage(updateTemp);
        latestDice = message.dice;
        sendMessage(message);

        setStatus(SENT_A);
    }

    private void sendMessageB () {
        testStatus(RECEIVED_A);

        LNPaymentBMessage message = paymentLogic.getBMessage();
        sendMessage(message);

        setStatus(SENT_B);

    }

    private void sendMessageC () {
        if (weStartedExchange) {
            testStatus(RECEIVED_B);
        } else {
            testStatus(RECEIVED_C);
        }

        LNPayment message = paymentLogic.getCMessage();
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
        LNPayment message = paymentLogic.getDMessage();
        sendMessage(message);

        setStatus(SENT_D);

        if (!weStartedExchange) {
            successCurrentTask();
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
            successCurrentTask();
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

        abortCountDown();
    }

    private void successCurrentTask () {
        this.updateTemp = paymentLogic.getChannelUpdate();
        currentQueueElement.clear();
        updatePaymentsDatabase();
        evaluateUpdates();
        channel = paymentLogic.updateChannel(channel);
        dbHandler.updateChannel(channel);
        eventHelper.onPaymentExchangeDone();

        finished = true;
        aborted = false;
        setStatus(IDLE);
        abortCountDown();
    }

    private void updatePaymentsDatabase () {
        for (PaymentData payment : updateTemp.newPayments) {
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
        for (PaymentData payment : updateTemp.refundedPayments) {
            PaymentWrapper wrapper = dbHandler.getPayment(payment.secret);
            if (weStartedExchange) {
                wrapper.statusReceiver = REFUNDED;
                dbHandler.updatePaymentReceiver(wrapper);
            } else {
                wrapper.statusSender = REFUNDED;
                dbHandler.updatePaymentSender(wrapper);
            }
        }
        for (PaymentData payment : updateTemp.redeemedPayments) {
            PaymentWrapper wrapper = dbHandler.getPayment(payment.secret);
            if (weStartedExchange) {
                wrapper.statusReceiver = REDEEMED;
                dbHandler.updatePaymentReceiver(wrapper);
            } else {
                wrapper.statusSender = REDEEMED;
                dbHandler.updatePaymentSender(wrapper);
            }
        }

    }

    private void evaluateUpdates () {
        channel.channelStatus.applyUpdate(updateTemp);
        if (!weStartedExchange) {
            for (PaymentData newPayment : updateTemp.newPayments) {
                paymentHelper.relayPayment(this, newPayment);
            }

            for (PaymentData redeemedPayment : updateTemp.redeemedPayments) {
                paymentHelper.paymentRedeemed(redeemedPayment.secret);
            }

            for (PaymentData refundedPayment : updateTemp.refundedPayments) {
                paymentHelper.paymentRefunded(refundedPayment);
            }
        }

    }

    private void sendMessage (Message message) {
        messageExecutor.sendMessageUpwards(message);
    }

    private void setStatus (Status status) {
        this.status = status;
    }

    @Override
    public void onInboundMessage (Message message) {
        try {
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
        } catch (LNPaymentException e) {
            sendMessage(messageFactory.getFailureMessage(e.getMessage()));
        }
    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        List<Channel> openChannel = dbHandler.getChannel(node.pubKeyClient);
        Preconditions.checkArgument(openChannel.size() > 0);

        this.channel = openChannel.get(0);
        paymentLogic.initialise(channel);

        this.messageExecutor = messageExecutor;
        this.paymentHelper.addProcessor(this);
        startQueueListener();
    }

    @Override
    public void onLayerClose () {
        this.connectionClosed = true;
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
        ChannelStatus statusTemp = channel.channelStatus.getClone();
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
