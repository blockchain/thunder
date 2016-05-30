package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.layer.high.channel.ChannelSignatures;

public interface LNSignatureMessage {
    ChannelSignatures getChannelSignatures ();
    void setChannelSignatures(ChannelSignatures channelSignatures);
}

