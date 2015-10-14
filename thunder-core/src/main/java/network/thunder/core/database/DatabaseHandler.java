package network.thunder.core.database;

import network.thunder.core.communication.Node;
import network.thunder.core.database.objects.Channel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by matsjerratsch on 14/10/2015.
 */
public class DatabaseHandler {

	/**
	 * Gets the active channels.
	 *
	 * @param conn the conn
	 * @return the active channels
	 * @throws SQLException the SQL exception
	 */
	public static ArrayList<Channel> getActiveChannels (Connection conn) throws SQLException {
		PreparedStatement stmt = null;
		ArrayList<Channel> channelList = new ArrayList<>();
		try {
			stmt = conn.prepareStatement("SELECT * FROM channels WHERE is_ready=1");

			ResultSet result = stmt.executeQuery();

			if (!result.first()) {
				return channelList;
			}

			while (!result.isAfterLast()) {
				Channel c = new Channel(result);
				channelList.add(c);
				result.next();
			}
			result.close();
			return channelList;
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Gets the active channels.
	 *
	 * @param conn the conn
	 * @return the active channels
	 * @throws SQLException the SQL exception
	 */
	public static ArrayList<Node> getNodesWithOpenChanels (Connection conn) throws SQLException {
		PreparedStatement stmt = null;
		ArrayList<Node> channelList = new ArrayList<>();
		try {
			stmt = conn.prepareStatement("SELECT nodes.host, nodes.port FROM nodes, channels WHERE channels.is_ready=1 AND node.id=channels.nodeid");

			ResultSet result = stmt.executeQuery();

			if (!result.first()) {
				return channelList;
			}

			while (!result.isAfterLast()) {
				Node node = new Node(result);
				channelList.add(node);
				result.next();
			}
			result.close();
			return channelList;
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

}
