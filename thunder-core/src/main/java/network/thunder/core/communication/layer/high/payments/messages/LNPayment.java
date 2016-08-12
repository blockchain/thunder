package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.high.AckMessage;
import network.thunder.core.communication.layer.high.AckableMessage;
import network.thunder.core.communication.layer.high.ChannelSpecificMessage;
import network.thunder.core.communication.layer.high.NumberedMessage;
import org.bitcoinj.core.Sha256Hash;

public abstract class LNPayment extends AckableMessage implements Message, AckMessage, NumberedMessage, ChannelSpecificMessage {
    private byte[] channelHash;
    private long messageNumberToAck;
    private long messageNumber;

    @Override
    public long getMessageNumberToAck () {
        return messageNumberToAck;
    }

    @Override
    public void setMessageNumberToAck (long number) {
        this.messageNumberToAck = number;
    }

    @Override
    public long getMessageNumber () {
        return messageNumber;
    }

    @Override
    public void setMessageNumber (long number) {
        this.messageNumber = number;
    }

    @Override
    public Sha256Hash getChannelHash () {
        return Sha256Hash.wrap(channelHash);
    }

    @Override
    public void setChannelHash (Sha256Hash hash) {
        this.channelHash = hash.getBytes();
    }

    @Override
    public String getMessageType () {
        return "LNPayment";
    }
}
