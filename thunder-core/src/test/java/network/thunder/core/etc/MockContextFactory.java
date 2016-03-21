package network.thunder.core.etc;

import network.thunder.core.communication.layer.ContextFactoryImpl;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.database.DBHandler;
import network.thunder.core.helper.wallet.MockWallet;
import network.thunder.core.communication.ServerObject;
import org.bitcoinj.core.Wallet;

/**
 * Created by matsjerratsch on 16/02/2016.
 */
public class MockContextFactory extends ContextFactoryImpl {

    public MockContextFactory (ServerObject node) {
        super(node, new DBHandlerMock(), new MockWallet(Constants.getNetwork()), new MockLNEventHelper());
    }

    public MockContextFactory (ServerObject node, DBHandler dbHandler) {
        super(node, dbHandler, new MockWallet(Constants.getNetwork()), new MockLNEventHelper());
    }

    public MockContextFactory (ServerObject node, DBHandler dbHandler, Wallet wallet, LNEventHelper eventHelper) {
        super(node, dbHandler, wallet, eventHelper);
    }
}
