package network.thunder.core.communication.layer;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.FailureMessage;

public class FailureMessageImpl implements FailureMessage {
    String failure;

    public FailureMessageImpl (String failure) {
        this.failure = failure;
    }

    @Override
    public String getFailure () {
        return failure;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(failure);
    }
}
