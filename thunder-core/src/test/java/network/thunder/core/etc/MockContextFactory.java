package network.thunder.core.etc;

import network.thunder.core.communication.objects.messages.impl.factories.ContextFactoryImpl;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNEventHelper;
import network.thunder.core.database.DBHandler;
import network.thunder.core.mesh.NodeServer;
import org.bitcoinj.core.Wallet;

/**
 * Created by matsjerratsch on 16/02/2016.
 */
public class MockContextFactory extends ContextFactoryImpl {

    public MockContextFactory (NodeServer node) {
        super(node, new DBHandlerMock(), new MockWallet(Constants.getNetwork()), new MockLNEventHelper());
    }

    public MockContextFactory (NodeServer node, DBHandler dbHandler) {
        super(node, dbHandler, new MockWallet(Constants.getNetwork()), new MockLNEventHelper());
    }

    public MockContextFactory (NodeServer node, DBHandler dbHandler, Wallet wallet, LNEventHelper eventHelper) {
        super(node, dbHandler, wallet, eventHelper);
    }
}
