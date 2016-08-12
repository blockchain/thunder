package network.thunder.core.communication.layer.high.payments

import network.thunder.core.communication.ClientObject
import network.thunder.core.communication.ServerObject
import network.thunder.core.communication.layer.ContextFactory
import network.thunder.core.communication.layer.DIRECTION.RECEIVED
import network.thunder.core.communication.layer.DIRECTION.SENT
import network.thunder.core.communication.layer.Message
import network.thunder.core.communication.layer.MessageExecutor
import network.thunder.core.communication.layer.MessageWrapper
import network.thunder.core.communication.layer.high.*
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic.SIDE.SERVER
import network.thunder.core.communication.layer.high.payments.messages.*
import network.thunder.core.communication.layer.high.payments.updates.PaymentNew
import network.thunder.core.communication.layer.high.payments.updates.PaymentRedeem
import network.thunder.core.communication.layer.high.payments.updates.PaymentRefund
import network.thunder.core.communication.processor.exceptions.LNPaymentException
import network.thunder.core.database.DBHandler
import network.thunder.core.etc.Constants
import network.thunder.core.etc.Tools
import network.thunder.core.helper.events.LNEventHelper
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Processor for negotiating new payment status.
 *
 * There are currently two allowed message exchanges.
 * A    ->
 *      <-      B
 * C    ->
 *
 * or
 *
 * A    ->
 *      <-      A
 * A    ->
 *      <-      B
 * C    ->
 *
 * in case of concurrent payment requests. In case of the conflict, the dice in the message decides which party may
 *      continue with his exchange, with the other party putting its own updates on hold.
 *
 * TODO Violation of this strict protocol (for example by not honoring the dice or sending another A message) will
 *      lead directly to channel closure.
 *
 * Message A contains the new revocation hash that is to be used with the exchange after the one right now.
 * This is important for the conflict case, as it means we can directly start a new exchange with new hashes.
 * When starting another exchange with the same hashes, the other party holds multiple valid states
 *      that are all signed by us, which means it can choose the most profitable one. More clearly, any payment
 *      cannot be considered final without it being the *only* valid state right now.
 */
class LNPaymentProcessorImpl(
        contextFactory: ContextFactory,
        var dbHandler: DBHandler,
        var node: ClientObject) : LNPaymentProcessor() {

    val log: Logger = Tools.getLogger()

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
                Thread.sleep(100)
            }
        }).start()
    }

    override fun ping() {
        if (executingLock.tryLock()) {
            try {
                val channelList = dbHandler.getOpenChannel(node.nodeKey)
                for (channel in channelList) {
                    //TODO support multiple channels..
                    val messages = dbHandler.getMessageList(node.nodeKey, channel.hash, "LNPayment").toList()
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
                val messages = dbHandler.getMessageList(node.nodeKey, message.channelHash, "LNPayment").toList()
                val channel = dbHandler.getChannel(message.channelHash).copy()
                val state = PaymentState(channel, messages)

                val (newChannel, newUpdate, oldRevocation, newMessage) =
                        if (message is LNPaymentAMessage) {
                            readMessageA(state, message)
                        } else if (message is LNPaymentBMessage) {
                            readMessageB(state, message)
                        } else {
                            readMessageC(state, message as LNPaymentCMessage)
                        }

                if (newUpdate != null) {
                    log.debug("New Update finalized: \n " + state.channel.toString() + " \n " + newChannel)
                }

                if (newMessage == null) {
                    dbHandler.unlockPayments(channel.hash);
                }

                var response: NumberedMessage? = null;
                if (newMessage != null) {
                    newMessage.messageNumberToAck = message.messageNumber
                    response = newMessage
                } else {
                    response = AckMessageImpl(message.messageNumber)
                }

                if (response is ChannelSpecificMessage) {
                    response.channelHash = message.channelHash
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

                if (newUpdate != null && newChannel != null) {
                    for ((i, p) in newUpdate.newPayments.withIndex()) {
                        val paymentData = newChannel.paymentList.get(newChannel.paymentList.size - i - 1);
                        if (!paymentData.sending) {
                            eventHelper.onPaymentAdded(node.nodeKey, paymentData);
                        }
                    }
                    for (paymentData in newUpdate.redeemedPayments) {
                        eventHelper.onPaymentRedeemed(channel.paymentList.get(paymentData.paymentIndex));
                    }
                    for (paymentData in newUpdate.refundedPayments) {
                        eventHelper.onPaymentRefunded(channel.paymentList.get(paymentData.paymentIndex));
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
        val paymentsRedeem = dbHandler.lockPaymentsToBeRedeemed(node.nodeKey)
        val paymentsRefund = dbHandler.lockPaymentsToBeRefunded(node.nodeKey)

        //TODO once we have multiple channels, we have to separate refunds/redemptions to their respective channels

        if (state.channel.paymentList.size > Constants.MAX_HTLC_PER_CHANNEL
                && paymentsRedeem.size == 0 && paymentsRefund.size == 0) {
            return null;
        }

        val paymentsRefundMapped = paymentsRefund.map { it.paymentId }.map { id -> state.channel.paymentList.indexOfFirst { it.paymentId == id } }.map { PaymentRefund(it) }

        val paymentsRedeemMapped = ArrayList<PaymentRedeem>()
        for (payment in paymentsRedeem) {
            val index = state.channel.paymentList.indexOfFirst { it.paymentId == payment.paymentId }
            paymentsRedeemMapped.add(PaymentRedeem(payment.secret, index))
        }

        val messageA = createMessageA(state, paymentsNew.map { it.paymentNew }, paymentsRefundMapped, paymentsRedeemMapped)
        if (messageA != null) {
            dbHandler.insertMessage(node.nodeKey, messageA, SENT)
            return messageA;
        } else {
            dbHandler.unlockPayments(state.channel.hash)
            return null
        }
    }

    fun createMessageA(state: PaymentState,
                       paymentsNew: List<PaymentNew>,
                       paymentsRefund: List<PaymentRefund>,
                       paymentsRedeem: List<PaymentRedeem>): LNPaymentAMessage? {

        //Let's add all redeems and refunds first, since they will always succeed
        var channel = state.channel
        val statusTemp = channel.copy()
        val update = ChannelUpdate()
        update.applyConfiguration(serverObject.configuration)
        for (data in paymentsRefund) {
            val paymentData = channel.paymentList.get(data.paymentIndex);
            if (paymentData != null) {
                statusTemp.amountClient += paymentData.amount
                update.refundedPayments.add(data)
            } else {
                throw RuntimeException("Want to refund a payment not included in current statusSender..");
            }
        }

        for (data in paymentsRedeem) {
            val paymentData = channel.paymentList.get(data.paymentIndex);
            if (paymentData != null) {
                statusTemp.amountServer += paymentData.amount
                update.redeemedPayments.add(data)
            } else {
                throw RuntimeException("Want to redeem a payment not included in current statusSender..");
            }
        }

        //Okay, now we can try to squeeze in as many payments as possible
        for (data in paymentsNew) {
            if (statusTemp.amountServer > data.amount) {
                statusTemp.amountServer -= data.amount
                update.newPayments.add(data)
            }
            if (update.newPayments.size + channel.paymentList.size > Constants.MAX_HTLC_PER_CHANNEL) {
                break;
            }
        }
        if (update.newPayments.size + update.redeemedPayments.size + update.refundedPayments.size == 0) {
            return null;
        }

        val status = channel.copy()
        val statusT = channel.copy()
        status.applyUpdate(update, true)
        statusT.applyUpdate(update, true)
        statusT.applyNextRevoHash()

        log.debug("Sending update: " + update)


        channel = status

        val message = LNPaymentAMessage(update)
        addSignatureToMessage(channel, message)
        addNewRevocationToMessage(channel, message)

        message.channelHash = channel.hash

        return message
    }


    fun readMessageA(state: PaymentState, message: LNPaymentAMessage): PaymentResponse {
        val lastMessage = state.messages.lastOrNull()

        val channel = applyUpdates(state, listOf(MessageWrapper(message, direction = RECEIVED)))

        if (lastMessage != null && lastMessage.message is LNPaymentAMessage) {
            if (lastMessage.direction == SENT) {
                if (message.dice < lastMessage.message.dice) {
                    //Our dice was higher, it's our turn to send yet another update with the new revocation hash received here
                    //TODO don't use the old update object, rather reconstruct it again from the database
                    val response = LNPaymentAMessage(lastMessage.message.channelUpdate)
                    addSignatureToMessage(channel, response)
                    addNewRevocationToMessage(channel, response)
                    return PaymentResponse(null, null, null, response)
                } else {
                    //Our dice was lower. We will wait for the other party to send another updated A message
                    return PaymentResponse(null, null, null, null)
                }
            }
        }
        paymentLogic.checkUpdate(serverObject.configuration, state.channel, message.channelUpdate)

        log.debug("Receiving update: " + message.channelUpdate)

        checkNewRevocationMessage(channel, message)
        checkSignatureMessage(channel, message)

        val outboundMessage = LNPaymentBMessage()
        addNewRevocationToMessage(channel, outboundMessage)
        addOldRevocationToMessage(channel, outboundMessage)
        addSignatureToMessage(channel, outboundMessage)

        return PaymentResponse(null, null, null, outboundMessage)
    }


    fun readMessageB(state: PaymentState, message: LNPaymentBMessage): PaymentResponse {
        val messageWrapper = state.messages.last { it.direction == SENT && it.message is LNPaymentAMessage }
        val messageA = messageWrapper.message as LNPaymentAMessage
        var channel = applyUpdates(state, listOf(MessageWrapper(message, direction = RECEIVED)))

        checkOldRevocationMessage(channel, message, state.channel)
        checkNewRevocationMessage(channel, message)
        checkSignatureMessage(channel, message)

        val outboundMessage = LNPaymentCMessage()
        addOldRevocationToMessage(channel, outboundMessage)

        channel = applyUpdates(state, listOf(MessageWrapper(message, direction = RECEIVED), MessageWrapper(outboundMessage, direction = SENT)))

        channel.applyNextRevoHash()
        channel.applyNextNextRevoHash()
        return PaymentResponse(channel, messageA.channelUpdate, message.oldRevocation, outboundMessage)
    }

    fun readMessageC(state: PaymentState, message: LNPaymentCMessage): PaymentResponse {
        val messageA = state.messages.last { it.direction == RECEIVED && it.message is LNPaymentAMessage }.message as LNPaymentAMessage
        val messageB = state.messages.last { it.direction == SENT && it.message is LNPaymentBMessage } //Not used, but good as extra type safety

        var channel = applyUpdates(state, listOf(MessageWrapper(message, direction = RECEIVED)))

        checkOldRevocationMessage(channel, message, state.channel)

        channel.applyNextRevoHash()
        channel.applyNextNextRevoHash()

        return PaymentResponse(channel, messageA.channelUpdate, message.oldRevocation, null)
    }

    fun addNewRevocationToMessage(channel: Channel, message: LNRevokeNewMessage) {
        val newRevocation = RevocationHash(channel.revoHashServerNext.index + 1, channel.masterPrivateKeyServer)
        newRevocation.secret = null //IMPORTANT: remove your secret before sending it
        message.newRevocationHash = newRevocation
    }

    fun checkNewRevocationMessage(channel: Channel, message: LNRevokeNewMessage) {
        val current = channel.revoHashServerNext.index
        if (message.newRevocationHash.index != current + 1) {
            throw LNPaymentException("New revocation hash does not append chain. New: ${message.newRevocationHash.index}. Current: ${current}")
        }
    }

    fun addOldRevocationToMessage(channel: Channel, message: LNRevokeOldMessage) {
        message.oldRevocationHash = (channel.shaChainDepthCurrent..channel.revoHashServerNext.index - 1).
                map { RevocationHash(it, channel.masterPrivateKeyServer) }
    }

    fun checkOldRevocationMessage(channel: Channel, message: LNRevokeOldMessage, oldChannel: Channel) {
        val currentRevocation = channel.revoHashClientCurrent
        val currentRevocationReceived = message.oldRevocationHash.first()

        if (!currentRevocationReceived.check() || currentRevocation != currentRevocationReceived) {
            throw LNPaymentException("Old revocation check failed. ${currentRevocation} != ${currentRevocationReceived}")
        }

        if (message.oldRevocationHash.size > 1) {
            val intermediateRevocation = oldChannel.revoHashClientNext
            val intermediateRevocationReceived = message.oldRevocationHash.last()

            if (!intermediateRevocationReceived.check() || intermediateRevocation != intermediateRevocationReceived) {
                throw LNPaymentException("Old revocation check failed")
            }
        }
    }

    fun addSignatureToMessage(channel: Channel, message: LNSignatureMessage) {
        message.channelSignatures = paymentLogic.getSignatureObject(channel)
    }

    fun checkSignatureMessage(channel: Channel, message: LNSignatureMessage) {
        paymentLogic.checkSignatures(channel, message.getChannelSignatures(), SERVER)
    }

    fun sendMessage(message: Message) {
        log.trace("${node.nodeKey} O message = ${message}")
        messageExecutor.sendMessageUpwards(message)
    }

    fun applyUpdates(state: PaymentState, newMessageWrapper: List<MessageWrapper>): Channel {
        val channel = state.channel

        var messageList = state.messages
        val indexThisExchangeStarts = messageList.indexOfLast { it.message is LNPaymentCMessage }
        if (indexThisExchangeStarts != -1) {
            messageList = messageList.subList(indexThisExchangeStarts + 1, messageList.size);
        }

        messageList = messageList.toMutableList()
        messageList.addAll(newMessageWrapper)

        checkMessageOrder(messageList)


        var countA = 0
        for (messageWrapper in messageList) {
            val message = messageWrapper.message
            if (message is LNPaymentAMessage) countA++

            if (message is LNSignatureMessage) {
                if (messageWrapper.direction == RECEIVED) {
                    channel.channelSignatures = message.getChannelSignatures()
                }
            }

            if (message is LNRevokeNewMessage) {
                if (messageWrapper.direction == RECEIVED) {
                    channel.revoHashClientNextNext = message.newRevocationHash
                } else {
                    channel.revoHashServerNextNext = message.newRevocationHash
                }
            }
            if (countA == 2) {
                channel.applyNextNextRevoHash()
            }

            if (message is LNRevokeOldMessage && messageWrapper.direction == SENT) {
                channel.shaChainDepthCurrent = message.oldRevocationHash.map { it.index }.max()!! + 1
            }
        }

        val messageA: MessageWrapper
        if (messageList.count { it.message is LNPaymentAMessage } == 2) {
            messageA = messageList.get(0)
        } else {
            messageA = messageList.last { it.message is LNPaymentAMessage }
        }

        if (messageA.message is LNPaymentAMessage) {
            channel.applyUpdate(messageA.message.channelUpdate, messageA.direction != RECEIVED)
        }

        channel.timestampForceClose = channel.paymentList.map { it.timestampRefund }.min() ?: 0

        return channel
    }

    fun checkMessageOrder(messageList: List<MessageWrapper>) {
        //There are only 2 possiblities allowed
        //A -> A -> A -> B -> C
        //  or
        //A -> B -> C
        val conflictWon: Boolean
        val weStartedExchange: Boolean

        val messageA = messageList.get(0)
        if (messageA.message !is LNPaymentAMessage) throw LNPaymentException("Wrong message order")
        weStartedExchange = messageA.direction == SENT

        if (messageList.size == 1) return
        if (messageList.get(1).message is LNPaymentAMessage) {
            //Conflicting concurrent updates..
            val messageA1 = messageList.get(1)
            if (messageA1.message !is LNPaymentAMessage) throw LNPaymentException("Wrong message order")

            conflictWon = messageA.message.dice > messageA1.message.dice

            if (!conflictWon) {
                Tools.testMessageWrapper(messageList, LNPaymentAMessage::class.java, 2, RECEIVED, weStartedExchange)
                Tools.testMessageWrapper(messageList, LNPaymentBMessage::class.java, 3, SENT, weStartedExchange)
                Tools.testMessageWrapper(messageList, LNPaymentCMessage::class.java, 4, RECEIVED, weStartedExchange)
            } else {
                Tools.testMessageWrapper(messageList, LNPaymentAMessage::class.java, 2, SENT, weStartedExchange)
                Tools.testMessageWrapper(messageList, LNPaymentBMessage::class.java, 3, RECEIVED, weStartedExchange)
                Tools.testMessageWrapper(messageList, LNPaymentCMessage::class.java, 4, SENT, weStartedExchange)
            }
            if (messageList.size > 5) {
                throw LNPaymentException("Message after complete exchange..")
            }
        } else {
            //Normal case
            Tools.testMessageWrapper(messageList, LNPaymentBMessage::class.java, 1, RECEIVED, weStartedExchange)
            Tools.testMessageWrapper(messageList, LNPaymentCMessage::class.java, 2, SENT, weStartedExchange)
            if (messageList.size > 3) throw LNPaymentException("Message after complete exchange..")
        }
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

