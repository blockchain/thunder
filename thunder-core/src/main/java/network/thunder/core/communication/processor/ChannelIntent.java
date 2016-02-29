package network.thunder.core.communication.processor;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public enum ChannelIntent {
    GET_IPS(1),
    GET_SYNC_DATA(2),
    OPEN_CHANNEL(4),
    MAINTAIN_CHANNEL(8),
    MISC(16);

    ChannelIntent (int i) {
        this.state = i;
    }

    @Override
    public String toString () {
        return "ChannelIntent{" +
                "state=" + state +
                '}';
    }

    int state = 0;
}
