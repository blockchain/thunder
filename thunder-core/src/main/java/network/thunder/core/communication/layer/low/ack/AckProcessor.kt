package network.thunder.core.communication.layer.low.ack

import network.thunder.core.communication.ClientObject
import network.thunder.core.communication.ServerObject
import network.thunder.core.communication.layer.*
import network.thunder.core.communication.layer.high.AckMessage
import network.thunder.core.communication.layer.high.AckMessageImpl
import network.thunder.core.communication.layer.high.AckableMessage
import network.thunder.core.communication.layer.high.NumberedMessage
import network.thunder.core.database.DBHandler
import network.thunder.core.etc.Constants
import network.thunder.core.etc.Tools
import network.thunder.core.helper.events.LNEventHelper

abstract class AckProcessor : Processor() {

}

class AckProcessorImpl(
        contextFactory: ContextFactory,
        var dbHandler: DBHandler,
        var node: ClientObject) : AckProcessor() {
    private val log = Tools.getLogger()

    lateinit var messageExecutor: MessageExecutor

    val eventHelper: LNEventHelper
    val serverObject: ServerObject

    var connectionOpen = true


    init {
        this.eventHelper = contextFactory.eventHelper
        this.serverObject = contextFactory.serverSettings
    }

    override fun onLayerActive(messageExecutor: MessageExecutor?) {
        this.messageExecutor = messageExecutor!!
        startResendThread()
        this.messageExecutor.sendNextLayerActive()
    }

    override fun onInboundMessage(message: Message?) {
        if (message is AckMessageImpl) {
            //AckMessageImpl is the message type of this processor, it should not get passed further down the pipeline, we process it directly
            dbHandler.setMessageAcked(node.nodeKey, message.messageNumberToAck)
            dbHandler.insertMessage(node.nodeKey, message, DIRECTION.RECEIVED)
            return
        }
        if (message is NumberedMessage) {
            val lastMessage = dbHandler.lastProcessedMessaged(node.nodeKey)

            log.debug("Last Message: ${lastMessage} Received Message number: ${message.messageNumber}");

            if (lastMessage >= message.messageNumber) {
                val response = dbHandler.getMessageResponse(node.nodeKey, message.messageNumber)
                if (response != null) {
                    log.trace("response to "+message+ " was "+response)
                    //We have responded to that message already, send out the old response that we have on file
                    messageExecutor.sendMessageUpwards(response)
                    return
                } else {
                    throw RuntimeException("Should have a response on file..")
                }
            } else if (message.messageNumber != 1L && message.messageNumber > lastMessage + 1) {
                log.trace("Return without doing anything.. "+message.messageNumber)
                //We don't care about messages that are not immediate successor of the last message we processed
                //In the future we could keep them and play them in when its their turn.
                return
            }
        }
        if (message is AckMessage && message.messageNumberToAck > 0) {
            dbHandler.setMessageAcked(node.nodeKey, message.messageNumberToAck)
        }
        messageExecutor.sendMessageDownwards(message)
    }


    override fun onLayerClose() {
        connectionOpen = false
    }

    override fun consumesOutboundMessage(`object`: Any?): Boolean {
        return false
    }

    fun startResendThread() {
        Thread(Runnable({
            while (connectionOpen) {
                resendMessage()
                Thread.sleep(Constants.MESSAGE_RESEND_TIME)
            }
        })).start()
    }

    fun resendMessage() {
        val messages = dbHandler.getUnackedMessageList(node.nodeKey)
        if (messages != null) {
            for (m in messages) {
                if (m is AckableMessage) {
                    messageExecutor.sendMessageUpwards(m)
                }
            }
        }
    }
}