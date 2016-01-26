package network.thunder.core.communication.nio;

/**
 * Created by matsjerratsch on 22/01/2016.
 */
public interface ConnectionManager {
    public int getPort();

    public String getHostname();

    public void startUp() throws Exception;
}
