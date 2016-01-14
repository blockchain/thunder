package network.thunder.core.communication.processor.implementations.lnpayment;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.OnionObject;
import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.communication.processor.implementations.lnpayment.helper.*;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentProcessor;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.mesh.Node;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static network.thunder.core.communication.processor.implementations.lnpayment.LNPaymentProcessorImpl.Status.*;

/**
 * Created by matsjerratsch on 04/01/2016.
 */
public class LNPaymentProcessorImpl implements LNPaymentProcessor {
    final static int TIMEOUT_NEGOTIATION = 1 * 1000;
    final static int TIME_MAX_WAIT = 1000;

    LNPaymentMessageFactory messageFactory;
    Node node;
    LNPaymentLogic paymentLogic;
    DBHandler dbHandler;

    MessageExecutor messageExecutor;
    LNPaymentHelper paymentHelper;

    Channel channel;

    Status status = IDLE;

    LinkedBlockingDeque<QueueElement> queueList = new LinkedBlockingDeque<>(1000);
    QueueElement currentQueueElement;
    ChannelStatus channelStatus;
    boolean aborted = false;
    boolean finished = false;

    boolean weStartedExchange = false;

    CountDownLatch countDownLatch = new CountDownLatch(1);
    long currentTaskStarted;

    int latestDice = 0;

    public LNPaymentProcessorImpl (LNPaymentMessageFactory messageFactory, Node node, LNPaymentLogic paymentLogic, DBHandler dbHandler) {
        this.messageFactory = messageFactory;
        this.node = node;
        this.paymentLogic = paymentLogic;
        this.dbHandler = dbHandler;

        channel = dbHandler.getChannel(node);
    }

    private void startQueueListener () {
        new Thread(() -> {
            while (true) {
                try {

                    if (status == IDLE) {
                        QueueElement element = queueList.poll(100, TimeUnit.MILLISECONDS);
                        if (element != null) {
                            if (status == IDLE) {
                                currentQueueElement = element;
                                channelStatus = currentQueueElement.produceNewChannelStatus(channel.channelStatus);
                                sendMessageA();
                                restartCountDown(TIMEOUT_NEGOTIATION);

                                if (weStartedExchange && status != IDLE) {
                                    //Time is over - we try it again after some random delay?
                                    queueList.addFirst(currentQueueElement);

                                    setStatus(IDLE);
//                                    Thread.sleep(new Random().nextInt(TIME_MAX_WAIT));
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

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
    public boolean connectsToNodeId (byte[] nodeId) {
        return Arrays.equals(nodeId, node.pubKeyClient.getPubKey());
    }

    @Override
    public void makePayment (PaymentData paymentData, OnionObject onionObject) {
        QueueElementPayment payment = new QueueElementPayment();
        payment.paymentData = paymentData;
        queueList.add(payment);
    }

    @Override
    public void redeemPayment (PaymentData paymentData) {
        QueueElementRedeem payment = new QueueElementRedeem();
        //TODO
        queueList.add(payment);
    }

    @Override
    public void refundPayment (PaymentData paymentData) {
        QueueElementRefund payment = new QueueElementRefund();
        //TODO
        queueList.add(payment);
    }

    public void abortCurrentExchange () {
        System.out.println(node.name + " ABORT");
        abortCountDown();
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
    public void onOutboundMessage (Message message) {

    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
        startQueueListener();
    }

    private void sendMessageA () {
        testStatus(IDLE);
        weStartedExchange = true;

        LNPaymentAMessage message = messageFactory.getMessageA(channel, channelStatus);
        paymentLogic.putCurrentRevocationHashServer(message.newRevocation);
        paymentLogic.putNewChannelStatus(channelStatus);
        latestDice = message.dice;
        sendMessage(message);

        setStatus(SENT_A);
    }

    private void sendMessageB () {
        testStatus(RECEIVED_A);

        LNPaymentBMessage message = messageFactory.getMessageB(channel);
        paymentLogic.putCurrentRevocationHashServer(message.newRevocation);
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
        Message message = messageFactory.getMessageC(channel, paymentLogic.getClientTransaction());
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
        Message message = messageFactory.getMessageD(channel);
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

        paymentLogic.checkMessage(message);

        setStatus(RECEIVED_A);
        sendMessageB();
    }

    private void readMessageB (LNPaymentBMessage message) {
        testStatus(SENT_A);

        paymentLogic.checkMessage(message);
        setStatus(RECEIVED_B);
        sendMessageC();

    }

    private void readMessageC (LNPaymentCMessage message) {
        if ((weStartedExchange && status != SENT_C) || (!weStartedExchange && status != SENT_B)) {
            //TODO ERROR
            System.out.println(node.name + " ERROR READ MESSAGE C");

        } else {
            paymentLogic.checkMessage(message);
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
            paymentLogic.checkMessage(message);
            setStatus(RECEIVED_D);
        }
        if (weStartedExchange) {
            finishCurrentTask();
        } else {
            sendMessageD();
        }
    }

    public void testStatus (Status expected) {
//        System.out.println(node.name + " Expected " + expected + ". Was: " + status);

        if (status != expected) {
            throw new RuntimeException("Expected " + expected + ". Was: " + status);
        }
    }

    private void abortCurrentTask () {
        aborted = true;
        queueList.addFirst(currentQueueElement);

        //TODO
        weStartedExchange = false;
        currentQueueElement = new QueueElementUpdate();

        System.out.println(node.name + " abortCurrentTask");

        abortCountDown();

    }

    private void finishCurrentTask () {
        finished = true;
        aborted = false;
        setStatus(IDLE);
        System.out.println(node.name + " finishCurrentTask");
        abortCountDown();
    }

    private void sendMessage (Message message) {
        messageExecutor.sendMessageUpwards(message);
    }

    private void setStatus (Status status) {
        System.out.println(node.name + " SET STATUS FROM " + this.status + " TO " + status);
        this.status = status;
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
