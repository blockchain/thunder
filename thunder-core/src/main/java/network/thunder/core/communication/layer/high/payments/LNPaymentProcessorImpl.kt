package network.thunder.core.communication.layer.high.payments

import network.thunder.core.communication.ClientObject
import network.thunder.core.communication.ServerObject
import network.thunder.core.communication.layer.ContextFactory
import network.thunder.core.communication.layer.DIRECTION.RECEIVED
import network.thunder.core.communication.layer.DIRECTION.SENT
import network.thunder.core.communication.layer.Message
import network.thunder.core.communication.layer.MessageExecutor
import network.thunder.core.communication.layer.high.AckMessageImpl
import network.thunder.core.communication.layer.high.NumberedMessage
import network.thunder.core.communication.layer.high.RevocationHash
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic.SIDE.SERVER
import network.thunder.core.communication.layer.high.payments.messages.*
import network.thunder.core.communication.processor.exceptions.LNPaymentException
import network.thunder.core.database.DBHandler
import network.thunder.core.etc.Constants
import network.thunder.core.etc.Tools
import network.thunder.core.helper.events.LNEventHelper
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class LNPaymentProcessorImpl(
        contextFactory: ContextFactory,
        var dbHandler: DBHandler,
        var node: ClientObject) : LNPaymentProcessor() {

    val paymentLogic: LNPaymentLogic
    val paymentHelper: LNPaymentHelper
    val eventHelper: LNEventHelper
    val serverObject: ServerObject

    lateinit var messageExecutor: MessageExecutor

    val executingLock = ReentrantLock()

    var connected = true

    init {
        this.paymentLogic = contextFactory.lnPaymentLogic
        this.paymentHelper = contextFactory.paymentHelper
        this.eventHelper = contextFactory.eventHelper
        this.serverObject = contextFactory.serverSettings
    }

    fun startPingThread() {
        Thread(Runnable {
            while (connected) {
                ping()
                Thread.sleep(1000)
            }
        }).start()
    }

    override fun ping() {
        if (executingLock.tryLock()) {
            try {
                val channelList = dbHandler.getChannel(node.nodeKey)
                for (channel in channelList) {
                    //TODO support multiple channels..
                    val messages = dbHandler.getMessageList(node.nodeKey, channel.hash, LNPayment::class.java).toList()
                    val state = PaymentState(channel.copy(), messages)
                    if (messages.isEmpty() || messages.last().message is LNPaymentCMessage || messages.last().message is AckMessageImpl) {
                        val newMessage = startNewExchange(state, dbHandler)
                        if (newMessage != null) {
                            sendMessage(newMessage)
                            return
                        }
                    }
                }
            } finally {
                executingLock.unlock()
            }
        }
    }

    override fun onInboundMessage(message: Message) {
        executingLock.tryLock(1, TimeUnit.MINUTES)
        try {
            if (message is LNPayment) {
                val messages = dbHandler.getMessageList(node.nodeKey, message.channelHash, LNPayment::class.java).toList()
                val channel = dbHandler.getChannel(message.channelHash).copy()
                val state = PaymentState(channel, messages)

                var (newChannel, newUpdate, oldRevocation, newMessage) =
                        if (message is LNPaymentAMessage) {
                            readMessageA(state, message)
                        } else if (message is LNPaymentBMessage) {
                            readMessageB(state, message)
                        } else {
                            readMessageC(state, message as LNPaymentCMessage)
                        }


                var response: NumberedMessage? = null;
                if (newMessage != null) {
                    newMessage.messageNumberToAck = message.messageNumber
                    response = newMessage
                } else {
                    response = AckMessageImpl(message.messageNumber)
                }

                //One atomic transaction here
                dbHandler.updateChannelStatus(
                        node.nodeKey,
                        message.channelHash,
                        serverObject.pubKeyServer,
                        newChannel,
                        newUpdate?.clone,
                        oldRevocation,
                        message,
                        response)


                sendMessage(response)

                if (newUpdate != null) {
                    for (paymentData in newUpdate.newPayments.filter { !it.sending }) {
                        eventHelper.onPaymentAdded(node.nodeKey, paymentData);
                    }
                    for (paymentData in newUpdate.redeemedPayments) {
                        eventHelper.onPaymentRedeemed(paymentData);
                    }
                    for (paymentData in newUpdate.refundedPayments) {
                        eventHelper.onPaymentRefunded(paymentData);
                    }
                }


            } else {
                throw LNPaymentException("Wrong message? " + message);
            }
        } finally {
            executingLock.unlock()
        }
        if (executingLock.isLocked) {
            executingLock.unlock()
        }
        ping()
    }

    fun startNewExchange(state: PaymentState,
                         dbHandler: DBHandler): LNPaymentAMessage? {
        //We lock all payments that could get included into the next update
        //The ones that did not make it into this update will get released automatically upon completion
        //This prevents accidentally refunding the payment on the other side while adding it here..
        val paymentsNew = dbHandler.lockPaymentsToBeMade(node.nodeKey)
        val paymentsRefund = dbHandler.lockPaymentsToBeRefunded(node.nodeKey)
        val paymentsRedeem = dbHandler.lockPaymentsToBeRedeemed(node.nodeKey)

        //TODO once we have multiple channels, we have to separate refunds/redemptions to their respective channels

        if (state.channel.channelStatus.paymentList.size > Constants.MAX_HTLC_PER_CHANNEL
                && paymentsRedeem.size == 0 && paymentsRefund.size == 0) {
            return null;
        }

        if (paymentsNew.size + paymentsRedeem.size + paymentsRefund.size > 0) {
            val messageA = createMessageA(state, paymentsNew, paymentsRefund, paymentsRedeem)
            dbHandler.saveMessage(node.nodeKey, messageA, SENT)
            return messageA;
        } else {
            return null
        }
    }

    fun createMessageA(state: PaymentState,
                       paymentsNew: List<PaymentData>,
                       paymentsRefund: List<PaymentData>,
                       paymentsRedeem: List<PaymentData>): LNPaymentAMessage {

        //Let's add all redeems and refunds first, since they will always succeed
        val channel = state.channel
        val statusTemp = channel.channelStatus.copy()
        val update = ChannelUpdate()
        update.applyConfiguration(serverObject.configuration)
        for (data in paymentsRefund) {
            if (statusTemp.paymentList.remove(data)) {
                statusTemp.amountClient += data.amount
                update.refundedPayments.add(data)
                data.sending = false;
            } else {
                throw RuntimeException("Want to refund a payment not included in current statusSender..");
            }
        }

        for (data in paymentsRedeem) {
            if (statusTemp.paymentList.remove(data)) {
                statusTemp.amountServer += data.amount
                update.redeemedPayments.add(data)
                data.sending = false;
            } else {
                throw RuntimeException("Want to redeem a payment not included in current statusSender..");
            }
        }

        //Okay, now we can try to squeeze in as many payments as possible
        for (data in paymentsNew) {
            if (statusTemp.amountServer > data.amount) {
                data.timestampOpen = Tools.currentTime()
                data.sending = true;
                statusTemp.amountServer -= data.amount
                update.newPayments.add(data)
            }
            if (update.newPayments.size + channel.channelStatus.paymentList.size > Constants.MAX_HTLC_PER_CHANNEL) {
                break;
            }
        }
        val status = channel.channelStatus.copy()
        status.applyUpdate(update)
        status.applyNextRevoHash()

        println("Sending update: ")
        println("Old statusSender: " + state.channel.channelStatus)
        println("New statusSender: " + status)

        channel.channelStatus = status

        val signatures = paymentLogic.getSignatureObject(channel)

        val message = LNPaymentAMessage(update, signatures)
        message.channelHash = channel.hash

        return message
    }


    fun readMessageA(state: PaymentState, message: LNPaymentAMessage): PaymentResponse {
        val lastMessage = state.messages.lastOrNull()

        if (lastMessage != null && lastMessage.message is LNPaymentAMessage) {
            if (lastMessage.direction == SENT) {
                if (message.dice < lastMessage.message.dice) {
                    //Our dice was higher, we just ignore their message..
                    return PaymentResponse(null, null, null, null)
                } else {
                }
            } else {
                throw LNPaymentException("Wrong message received in readMessageA");
            }
        }

        //Only process the message if we completed the last exchange or if concurrent A exchange
        if (lastMessage == null || lastMessage.message is LNPaymentAMessage || lastMessage.message is LNPaymentCMessage) {
            val channel = state.channel
            paymentLogic.checkUpdate(serverObject.configuration, channel, message.channelUpdate.cloneReversed)

            channel.channelStatus.applyUpdate(message.channelUpdate.cloneReversed)
            channel.channelStatus.applyNextRevoHash();

            paymentLogic.checkSignatures(channel, message.getChannelSignatures(), SERVER)

            val signatures = paymentLogic.getSignatureObject(channel)

            val oldRevocation = RevocationHash(channel.shaChainDepth, channel.masterPrivateKeyServer)
            val newRevocation = RevocationHash(channel.shaChainDepth + 2, channel.masterPrivateKeyServer)
            newRevocation.secret = null //IMPORTANT: remove your secret before sending it


            val outboundMessage = LNPaymentBMessage(signatures, oldRevocation, newRevocation)
            outboundMessage.channelHash = message.channelHash

            return PaymentResponse(null, null, null, outboundMessage)
        } else {
            throw LNPaymentException("Wrong message received in readMessageA: " + lastMessage);
        }
    }


    fun readMessageB(state: PaymentState, message: LNPaymentBMessage): PaymentResponse {
        val messageA = state.messages.last { it.direction == SENT && it.message is LNPaymentAMessage }

        if (messageA.message !is LNPaymentAMessage || messageA.direction != SENT) {
            throw LNPaymentException("Wrong message received in readMessageB");
        }
        val channel = state.channel
        var status = channel.channelStatus

        if (!message.oldRevocation.check() || !Arrays.equals(state.channel.channelStatus.revoHashClientCurrent.secretHash, message.oldRevocation.secretHash)) {
            throw LNPaymentException("Old revocation check failed. " +
                    "Old one on file: ${channel.channelStatus.revoHashClientCurrent}. Received: ${message.oldRevocation}")
        }

        if (message.newRevocation.index != channel.channelStatus.revoHashClientNext.index + 1) {
            throw LNPaymentException("New revocation hash does not append chain")
        }

        status.applyUpdate(messageA.message.channelUpdate)
        status.applyNextRevoHash()
        paymentLogic.checkSignatures(channel, message.getChannelSignatures(), SERVER)

        val oldRevocation = RevocationHash(channel.shaChainDepth, channel.masterPrivateKeyServer)
        val newRevocation = RevocationHash(channel.shaChainDepth + 2, channel.masterPrivateKeyServer)
        newRevocation.secret = null //IMPORTANT: remove secret before sending it


        channel.channelSignatures = message.getChannelSignatures()
        channel.channelStatus = status
        channel.shaChainDepth++
        channel.channelStatus.revoHashClientNext = message.newRevocation
        channel.channelStatus.revoHashServerNext = newRevocation //TODO
        channel.timestampForceClose = status.paymentList.map { it.timestampRefund }.min() ?: 0


        val outboundMessage = LNPaymentCMessage(oldRevocation, newRevocation)
        outboundMessage.channelHash = message.channelHash

        return PaymentResponse(channel, messageA.message.channelUpdate, message.oldRevocation, outboundMessage)
    }

    fun readMessageC(state: PaymentState, message: LNPaymentCMessage): PaymentResponse {
        val messageA = state.messages.last { it.direction == RECEIVED && it.message is LNPaymentAMessage }.message as LNPaymentAMessage
        val messageB = state.messages.last { it.direction == SENT && it.message is LNPaymentBMessage }

        val channel = state.channel

        if (messageB.message !is LNPaymentBMessage || messageB.direction != SENT) {
            throw LNPaymentException("Wrong message received in readMessageC");
        }

        if (!message.oldRevocation.check() || !Arrays.equals(channel.channelStatus.revoHashClientCurrent.secretHash, message.oldRevocation.secretHash)) {
            throw LNPaymentException("Old revocation check failed")
        }

        if (message.newRevocation.index != channel.channelStatus.revoHashClientNext.index + 1) {
            throw LNPaymentException("New revocation hash does not append chain")
        }


        var status = state.channel.channelStatus.copy()
        status.applyUpdate(messageA.channelUpdate.cloneReversed)
        status.applyNextRevoHash()


        channel.channelSignatures = messageA.getChannelSignatures()
        channel.channelStatus = status
        channel.shaChainDepth++
        channel.channelStatus.revoHashClientNext = message.newRevocation
        channel.channelStatus.revoHashServerNext = messageB.message.newRevocation
        channel.timestampForceClose = status.paymentList.map { it.timestampRefund }.min() ?: 0

        return PaymentResponse(channel, messageA.channelUpdate.cloneReversed, message.oldRevocation, null)
    }

    fun sendMessage(m: Message) {
        messageExecutor.sendMessageUpwards(m)
    }


    override fun onLayerActive(messageExecutor: MessageExecutor) {
        this.messageExecutor = messageExecutor
        this.paymentHelper.addProcessor(node.nodeKey, this);
        startPingThread()
    }

    override fun onLayerClose() {
        this.paymentHelper.removeProcessor(node.nodeKey)
        connected = false
    }

    override fun consumesInboundMessage(`object`: Any): Boolean {
        return `object` is LNPayment
    }

    override fun consumesOutboundMessage(`object`: Any): Boolean {
        return false
    }
}
