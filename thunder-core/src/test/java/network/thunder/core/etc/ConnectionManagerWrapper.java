package network.thunder.core.etc;

import network.thunder.core.communication.ConnectionManager;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.database.DBHandler;
import org.bitcoinj.core.Wallet;

public class ConnectionManagerWrapper {
    public ContextFactory contextFactory;
    public Wallet wallet;
    public ConnectionManager connectionManager;
    public DBHandler dbHandler;
}
