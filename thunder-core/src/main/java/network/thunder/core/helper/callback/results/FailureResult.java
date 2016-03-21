package network.thunder.core.helper.callback.results;

public class FailureResult implements Result {

    String message;

    public FailureResult (String message) {
        this.message = message;
    }

    public FailureResult () {
    }

    @Override
    public boolean wasSuccessful () {
        return false;
    }

    @Override
    public String getMessage () {
        return message;
    }
}
