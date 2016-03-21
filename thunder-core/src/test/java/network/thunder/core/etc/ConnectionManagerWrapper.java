package network.thunder.core.etc;

import network.thunder.core.communication.ConnectionManager;
import network.thunder.core.database.DBHandler;
import org.bitcoinj.core.Wallet;

/**
 * Created by matsjerratsch on 26/01/2016.
 */
public class ConnectionManagerWrapper {
    public Wallet wallet;
    public ConnectionManager connectionManager;
    public DBHandler dbHandler;
}
