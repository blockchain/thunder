package network.thunder.core.communication.nio;

import network.thunder.core.communication.Node;
import network.thunder.core.database.DatabaseHandler;

import javax.sql.DataSource;
import java.util.ArrayList;

/**
 * Created by matsjerratsch on 14/10/2015.
 */
public class P2PContext {

	public static final String HOST = "127.0.0.1";
	public static final int PORT = 8892;

	private ArrayList<Node> connectedNodes = new ArrayList<>();

	P2PServer server = new P2PServer();
	P2PClient client = new P2PClient();

	public P2PContext(DataSource dataSource) throws Exception {

		//Open up for incoming connections
		server.startServer(8892, connectedNodes);


		ArrayList<Node> activeNodes = DatabaseHandler.getNodesWithOpenChanels(dataSource.getConnection());

		for(Node node : activeNodes) {
			new P2PClient().connectTo(node);
			connectedNodes.add(node);
		}


	}


}
