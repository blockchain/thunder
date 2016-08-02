package network.thunder.core.communication.layers.low

import io.netty.channel.embedded.EmbeddedChannel
import network.thunder.core.communication.ClientObject
import network.thunder.core.communication.ServerObject
import network.thunder.core.communication.layer.ContextFactoryImpl
import network.thunder.core.communication.layer.DIRECTION
import network.thunder.core.communication.layer.ProcessorHandler
import network.thunder.core.communication.layer.high.AckMessageImpl
import network.thunder.core.communication.layer.high.AckableMessage
import network.thunder.core.communication.layer.low.ack.AckProcessorImpl
import network.thunder.core.database.DBHandler
import network.thunder.core.database.inmemory.InMemoryDBHandler
import network.thunder.core.etc.Constants
import network.thunder.core.helper.events.LNEventHelperImpl
import network.thunder.core.helper.wallet.MockWallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AckProcessorImplTest {

    val RESEND_TIME = 100L

    val serverObject1 = ServerObject()
    val serverObject2 = ServerObject()

    val node1 = ClientObject(serverObject2)
    val node2 = ClientObject(serverObject1)

    val dbHandler1: DBHandler = InMemoryDBHandler()
    val dbHandler2: DBHandler = InMemoryDBHandler()

    val contextFactory1 = ContextFactoryImpl(serverObject1, dbHandler1, MockWallet(Constants.getNetwork()), LNEventHelperImpl());
    val contextFactory2 = ContextFactoryImpl(serverObject2, dbHandler2, MockWallet(Constants.getNetwork()), LNEventHelperImpl());

    val processor1 = AckProcessorImpl(contextFactory1, dbHandler1, node2)
    val processor2 = AckProcessorImpl(contextFactory2, dbHandler2, node1)

    lateinit var channel1: EmbeddedChannel
    lateinit var channel2: EmbeddedChannel

    @Before
    fun prepare() {
        Constants.MESSAGE_RESEND_TIME = RESEND_TIME

        channel1 = EmbeddedChannel(ProcessorHandler(processor1, "AckProcessor1"))
        channel2 = EmbeddedChannel(ProcessorHandler(processor2, "AckProcessor2"))

        assertNull(channel1.readOutbound());
        assertNull(channel2.readOutbound());
    }


    @Test
    fun shouldNotSendMessageBecauseTimeoutNotReachedYet() {
        dbHandler1.saveMessage(node2.nodeKey, AckableMessageMock(1), DIRECTION.SENT)
        Thread.sleep((Constants.MESSAGE_RESEND_TIME / 2).toLong())
        assertNull(channel1.readOutbound());
    }

    @Test
    fun shouldResendAllMessageAfterTimeout() {
        dbHandler1.saveMessage(node2.nodeKey, AckableMessageMock(1), DIRECTION.SENT)
        dbHandler1.saveMessage(node2.nodeKey, AckableMessageMock(2), DIRECTION.SENT)
        Thread.sleep((Constants.MESSAGE_RESEND_TIME * 2).toLong())
        assertEquals(1L, (channel1.readOutbound() as AckableMessage).getMessageNumber());
        assertEquals(2L, (channel1.readOutbound() as AckableMessage).getMessageNumber());
    }

    @Test
    fun shouldResendUnackedMessageAfterTimeout() {
        dbHandler1.saveMessage(node2.nodeKey, AckableMessageMock(1), DIRECTION.SENT)
        dbHandler1.saveMessage(node2.nodeKey, AckableMessageMock(2), DIRECTION.SENT)
        channel1.writeInbound(AckMessageImpl(1))
        Thread.sleep((Constants.MESSAGE_RESEND_TIME * 1.5).toLong())
        assertEquals(2L, (channel1.readOutbound() as AckableMessage).getMessageNumber());
        assertNull(channel1.readOutbound())
    }

    @Test
    fun shouldResendNoMessageAfterTimeout() {
        dbHandler1.saveMessage(node2.nodeKey, AckableMessageMock(1), DIRECTION.SENT)
        dbHandler1.saveMessage(node2.nodeKey, AckableMessageMock(2), DIRECTION.SENT)
        channel1.writeInbound(AckMessageImpl(1))
        channel1.writeInbound(AckMessageImpl(2))
        Thread.sleep((Constants.MESSAGE_RESEND_TIME * 1.5).toLong())
        assertNull(channel1.readOutbound())
    }

    @Test
    fun shouldNotSendMessageBecauseNothingSentYet() {
        Thread.sleep(200)
        assertNull(channel1.readOutbound());
    }

}

class AckableMessageMock(var number: Long = 1L) : AckableMessage() {

    override fun verify() {
    }

    override fun setMessageNumber(number: Long) {
        this.number = number
    }

    override fun getMessageNumber(): Long {
        return number
    }

}