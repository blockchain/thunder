package network.thunder.core.helper.callback.results;

public interface Result {

    public abstract boolean wasSuccessful ();

    public abstract String getMessage ();
}
