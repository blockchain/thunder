package network.thunder.core.helper.callback.results;

public class SuccessResult implements Result {
    @Override
    public boolean wasSuccessful () {
        return true;
    }

    @Override
    public String getMessage () {
        return null;
    }
}
