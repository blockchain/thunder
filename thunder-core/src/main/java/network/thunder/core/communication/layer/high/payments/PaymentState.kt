package network.thunder.core.communication.layer.high.payments

import network.thunder.core.communication.layer.MessageWrapper
import network.thunder.core.communication.layer.high.Channel

class PaymentState(
        private val channelInternal: Channel,
        val messages: List<MessageWrapper>
) {
    val channel: Channel
        get() = this.channelInternal.copy()

}