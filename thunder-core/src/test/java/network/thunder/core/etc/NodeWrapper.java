package network.thunder.core.etc;

import network.thunder.core.ThunderContext;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.HibernateHandler;
import network.thunder.core.helper.wallet.MockWallet;
import org.bitcoinj.wallet.Wallet;

public class NodeWrapper {
    public ServerObject serverObject;
    public ThunderContext thunderContext;
    public Wallet wallet;
    public DBHandler dbHandler;

    public void init (int port) {
        this.serverObject = new ServerObject();
        this.serverObject.hostServer = "localhost";
        this.serverObject.portServer = port;

        this.wallet = new MockWallet(Constants.getNetwork());

        this.dbHandler = new HibernateHandler();

        this.thunderContext = new ThunderContext(wallet, dbHandler, serverObject);
    }

}
