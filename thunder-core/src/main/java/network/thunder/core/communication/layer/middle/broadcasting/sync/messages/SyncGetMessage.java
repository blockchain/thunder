package network.thunder.core.communication.layer.middle.broadcasting.sync.messages;

/**
 * Created by matsjerratsch on 30/11/2015.
 */
public class SyncGetMessage implements Sync {
    public int fragmentIndex;

    public SyncGetMessage (int fragmentIndex) {
        this.fragmentIndex = fragmentIndex;
    }

    @Override
    public void verify () {

    }

    @Override
    public String toString () {
        return "SyncGetMessage{" +
                "fragmentIndex=" + fragmentIndex +
                '}';
    }
}
