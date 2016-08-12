package network.thunder.core.communication.layer;

public abstract class FailureMessage implements Message {
    public abstract String getFailure ();

    @Override
    public String getMessageType () {
        return "FailureMessage";
    }
}
