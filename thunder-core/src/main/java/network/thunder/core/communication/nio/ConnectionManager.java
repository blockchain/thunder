package network.thunder.core.communication.nio;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public interface ConnectionManager {
    public void startUp () throws Exception;

    public void startListening () throws Exception;
}
