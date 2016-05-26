package network.thunder.core.communication.layer

import network.thunder.core.etc.Tools

data class MessageWrapper(
        val message: Message,
        val timestamp: Int = Tools.currentTime(),
        val direction: DIRECTION = DIRECTION.SENT
)


enum class DIRECTION {RECEIVED, SENT }