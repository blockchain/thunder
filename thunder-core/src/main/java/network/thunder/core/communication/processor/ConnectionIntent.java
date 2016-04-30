package network.thunder.core.communication.processor;

public enum ConnectionIntent {
    GET_IPS(1),
    GET_SYNC_DATA(2),
    OPEN_CHANNEL(4),
    MAINTAIN_CHANNEL(8),
    MISC(16);

    ConnectionIntent (int i) {
        this.priority = i;
    }

    @Override
    public String toString () {
        return "ConnectionIntent{" +
                "priority=" + priority +
                '}';
    }

    public int getPriority () {
        return priority;
    }

    int priority = 0;
}
