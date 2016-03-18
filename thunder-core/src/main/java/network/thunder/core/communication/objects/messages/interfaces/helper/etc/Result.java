package network.thunder.core.communication.objects.messages.interfaces.helper.etc;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public interface Result {

    public abstract boolean wasSuccessful ();

    public abstract String getMessage ();
}
