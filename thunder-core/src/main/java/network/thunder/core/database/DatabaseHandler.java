package network.thunder.core.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import network.thunder.core.communication.Node;
import network.thunder.core.communication.objects.p2p.PubkeyChannelObject;
import network.thunder.core.communication.objects.p2p.PubkeyIPObject;
import network.thunder.core.database.objects.Channel;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by matsjerratsch on 14/10/2015.
 */
public class DatabaseHandler {

    public static void main(String[] args) throws Exception {
        fillPubkeyChannelTableWithRandomData(getDataSource().getConnection(), 110000);
    }

    public static DataSource getDataSource () throws PropertyVetoException {
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        cpds.setDriverClass("com.mysql.jdbc.Driver"); //loads the jdbc driver
        cpds.setJdbcUrl("jdbc:mysql://localhost/lightning?user=root");

        // the settings below are optional -- c3p0 can work with defaults
        cpds.setMinPoolSize(2);
        cpds.setAcquireIncrement(5);
        cpds.setMaxPoolSize(8);

        return cpds;
    }

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

    public static void fillPubkeyChannelTableWithRandomData (Connection conn, int entries) throws SQLException {

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO pubkey_channel VALUES(?,?,?)");

            Random random = new Random();
            for (int i = 0; i < entries; i++) {
                byte[] b = new byte[500];
                random.nextBytes(b);
                int index = random.nextInt(999) + 1;
                stmt.setString(1, null);
                stmt.setInt(2, index);
                stmt.setBytes(3, b);
                stmt.addBatch();
            }
            stmt.executeBatch();

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static ArrayList<PubkeyChannelObject> getPubkeyChannelObjectsByFragmentIndex (Connection conn, int index) throws SQLException {
        PreparedStatement stmt = null;
        ArrayList<PubkeyChannelObject> channelObjectArrayList = new ArrayList<>();
        try {
            stmt = conn.prepareStatement("SELECT * FROM pubkey_channel WHERE fragment_index=?");

            stmt.setInt(1, index);

            ResultSet result = stmt.executeQuery();

            if (!result.first()) {
                return channelObjectArrayList;
            }

            while (!result.isAfterLast()) {
                PubkeyChannelObject pubkeyChannelObject = new PubkeyChannelObject();
                pubkeyChannelObject.pubkeyA = result.getBytes("blob"); //TODO: Fix..
                channelObjectArrayList.add(pubkeyChannelObject);
                result.next();
            }
            result.close();
            return channelObjectArrayList;
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

    /**
     * Gets the active channels.
     *
     * @param conn the conn
     * @return the active channels
     * @throws SQLException the SQL exception
     */
    public static ArrayList<PubkeyIPObject> getIPAddresses (Connection conn) {
        try {
            PreparedStatement stmt = null;
            ArrayList<PubkeyIPObject> IPList = new ArrayList<>();
            try {
                stmt = conn.prepareStatement("SELECT * FROM nodes, ips WHERE nodes.id=ips.node_id");

                ResultSet result = stmt.executeQuery();

                if (!result.first()) {
                    return IPList;
                }

                while (!result.isAfterLast()) {
                    PubkeyIPObject node = new PubkeyIPObject(result);
                    IPList.add(node);
                    result.next();
                }
                result.close();
                return IPList;
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
