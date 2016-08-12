package network.thunder.core.communication.layer.middle.broadcasting.sync.messages;

public class SyncGetMessage extends Sync {
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
