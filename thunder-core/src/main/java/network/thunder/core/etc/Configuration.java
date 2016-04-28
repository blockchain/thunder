package network.thunder.core.etc;

import java.util.HashSet;
import java.util.Set;

public class Configuration {
    public String publicKey;
    public String serverKey;
    public String hostnameServer;
    public int portServer;
    public Set<String> nodesToBuildChannelWith = new HashSet<>();
}
