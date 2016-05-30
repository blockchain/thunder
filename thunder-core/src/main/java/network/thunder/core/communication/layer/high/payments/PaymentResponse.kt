package network.thunder.core.communication.layer.high.payments

import network.thunder.core.communication.layer.high.Channel
import network.thunder.core.communication.layer.high.RevocationHash
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate
import network.thunder.core.communication.layer.high.payments.messages.LNPayment

data class PaymentResponse(
        val channel: Channel?,
        val update: ChannelUpdate?,
        val revocationHash: List<RevocationHash>?,
        val messages: LNPayment?)

