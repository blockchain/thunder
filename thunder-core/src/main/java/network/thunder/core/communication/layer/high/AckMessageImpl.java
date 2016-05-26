package network.thunder.core.communication.layer.high;

import com.google.common.base.Preconditions;

public class AckMessageImpl implements AckMessage, NumberedMessage {
    long messageNumberToAck;
    long messageNumber;

    public AckMessageImpl (long messageNumberToAck) {
        this.messageNumberToAck = messageNumberToAck;
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
    public long getMessageNumberToAck () {
        return messageNumberToAck;
    }

    @Override
    public void setMessageNumberToAck (long number) {
        this.messageNumberToAck = number;
    }

    @Override
    public void verify () {
        Preconditions.checkArgument(messageNumberToAck != 0);
    }

    @Override
    public String toString () {
        return "AckMessageImpl{" +
                "messageNumberToAck=" + messageNumberToAck +
                '}';
    }
}
