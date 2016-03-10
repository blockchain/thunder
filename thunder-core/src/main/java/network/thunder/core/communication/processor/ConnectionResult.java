package network.thunder.core.communication.processor;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public enum ConnectionResult {
    UNKNOWN(0),
    CHANNEL_CLOSED(1),
    INCOMPATIBLE(2),
    SUCCESS(3),
    ERROR(4);

    ConnectionResult (int i) {
        this.state = i;
    }

    public boolean shouldTryToReconnect () {
        if (state == 4 || state == 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString () {
        return "ConnectionResult{" +
                "state=" + this +
                '}';
    }

    int state = 0;
}
