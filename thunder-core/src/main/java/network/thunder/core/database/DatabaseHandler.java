package network.thunder.core.database;

import com.mchange.v2.c3p0.C3P0ProxyStatement;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import network.thunder.core.communication.objects.p2p.DataObject;
import network.thunder.core.communication.objects.p2p.P2PDataObject;
import network.thunder.core.communication.objects.p2p.TreeMapDatastructure;
import network.thunder.core.communication.objects.p2p.sync.ChannelStatusObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyChannelObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyIPObject;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.Node;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by matsjerratsch on 14/10/2015.
 */
public class DatabaseHandler {

    public static void main (String[] args) throws Exception {
//        fillNodeTableWithRandomData(getDataSource().getConnection(), 200000);
//        fillChannelTableWithRandomData(getDataSource().getConnection(), 500000);
//        fillChannelStatusTableWithRandomData(getDataSource().getConnection(), 500000);
        fillPubkeyIPTableWithRandomData(getDataSource().getConnection(), 200000);
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

    public static int getChannelId (Connection conn, byte[] pubkeyA, byte[] pubkeyB) throws SQLException {

        PreparedStatement stmt = null;
        try {

//            int nodeAID = DatabaseHandler.getNodeId(conn, pubkeyA);
//            int nodeBID = DatabaseHandler.getNodeId(conn, pubkeyB);

//            //Lets see if we have the object in the db already
            stmt = conn.prepareStatement("SELECT channels.id FROM channels " +
                                             "INNER JOIN nodes AS nodes_a_table ON nodes_a_table.id = channels.node_id_a " +
                                             "INNER JOIN nodes AS nodes_b_table ON nodes_b_table.id = channels.node_id_b " +
                                             "WHERE ((nodes_a_table.pubkey = ? AND nodes_b_table.pubkey=?) OR (nodes_a_table.pubkey = ? AND nodes_b_table.pubkey=?))");

            stmt.setBytes(1, pubkeyA);
            stmt.setBytes(2, pubkeyB);
            stmt.setBytes(3, pubkeyB);
            stmt.setBytes(4, pubkeyA);

//            //Lets see if we have the object in the db already
//            stmt = conn.prepareStatement("SELECT channels.id FROM channels " + "WHERE ((node_id_a = ? AND node_id_b=?) OR (node_id_a = ? AND node_id_b=?))");
//
//            stmt.setInt(1, nodeAID);
//            stmt.setInt(2, nodeBID);
//            stmt.setInt(3, nodeBID);
//            stmt.setInt(4, nodeAID);

//            try {
//                printInnerStatement((C3P0ProxyStatement) stmt);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }

            ResultSet set = stmt.executeQuery();

            if (!set.first()) {
                return 0;
            }

            return set.getInt("channels.id");

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    static void printInnerStatement (C3P0ProxyStatement stmt) {
//        try {
//            java.lang.reflect.Method m = java.io.PrintStream.class.getMethod("println", new Class[]{Object.class});
//            stmt.rawStatementOperation(m, System.out, new Object[]{C3P0ProxyStatement.RAW_STATEMENT});
//        } catch (Exception e) {
//        }
    }

    public static int getNodeId (Connection conn, byte[] pubkey) throws SQLException {

        PreparedStatement stmt = null;
        try {

            //Lets see if we have the object in the db already
            stmt = conn.prepareStatement("SELECT nodes.id FROM nodes WHERE nodes.pubkey=?");

            stmt.setBytes(1, pubkey);

            ResultSet set = stmt.executeQuery();

            if (!set.first()) {
                //New node. Might as well just insert it now?
                stmt.close();
                set.close();

                stmt = conn.prepareStatement("INSERT INTO nodes VALUES(?,?)", PreparedStatement.RETURN_GENERATED_KEYS);

                stmt.setInt(1, 0);
                stmt.setBytes(2, pubkey);

                stmt.execute();

                set = stmt.getGeneratedKeys();
                set.first();
                int id = set.getInt(1);

                set.close();
                return id;
            }

            int id = set.getInt("nodes.id");
            return id;

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static int getIPId (Connection conn, byte[] pubkey) throws SQLException {

        PreparedStatement stmt = null;
        try {

            //Lets see if we have the object in the db already
            stmt = conn.prepareStatement("SELECT pubkey_ips.id FROM pubkey_ips" +
                                             " INNER JOIN nodes ON nodes.id=pubkey_ips.node_id" +
                                             " WHERE nodes.pubkey=?");

            stmt.setBytes(1, pubkey);

            ResultSet set = stmt.executeQuery();

            if (!set.first()) {
                return 0;
            }
            int id = set.getInt("pubkey_ips.id");
            set.close();
            return id;

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static int newChannel (Connection conn, PubkeyChannelObject pubkeyChannelObject) throws SQLException {

        PreparedStatement stmt = null;
        try {

            //Check if we have the channel already..
            int id = getChannelId(conn, pubkeyChannelObject.pubkeyA, pubkeyChannelObject.pubkeyB);

            if (id != 0) {
                //TODO: We already have this channel in our database. Maybe change the object? These should be fairly static though..
                stmt = conn.prepareStatement("UPDATE channels SET fragment_index=?, secret_a_hash=?, secret_b_hash=?, pubkey_a1=?, pubkey_a2=?, pubkey_b1=?, " +
                                                 "pubkey_b2=?, " +
                                                 "txid_anchor=?, signature_a=?, signature_b=?, hash=? WHERE id=?");

                stmt.setInt(1, TreeMapDatastructure.objectToFragmentIndex(pubkeyChannelObject));
                stmt.setBytes(2, pubkeyChannelObject.secretAHash);
                stmt.setBytes(3, pubkeyChannelObject.secretBHash);
                stmt.setBytes(4, pubkeyChannelObject.pubkeyA1);
                stmt.setBytes(5, pubkeyChannelObject.pubkeyA2);
                stmt.setBytes(6, pubkeyChannelObject.pubkeyB1);
                stmt.setBytes(7, pubkeyChannelObject.pubkeyB2);
                stmt.setBytes(8, pubkeyChannelObject.txidAnchor);
                stmt.setBytes(9, pubkeyChannelObject.signatureA);
                stmt.setBytes(10, pubkeyChannelObject.signatureB);
                stmt.setBytes(11, pubkeyChannelObject.getHash());
                stmt.setInt(12, id);

                printInnerStatement((C3P0ProxyStatement) stmt);
                stmt.execute();

                //TODO: Actually return the newly generated id..
                return id;
            }

            //We don't have the channel currently.. Let's insert it
            int nodeIdA = DatabaseHandler.getNodeId(conn, pubkeyChannelObject.pubkeyA);
            int nodeIdB = DatabaseHandler.getNodeId(conn, pubkeyChannelObject.pubkeyB);

            stmt = conn.prepareStatement("INSERT INTO channels VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            stmt.setInt(i++, id);
            stmt.setInt(i++, TreeMapDatastructure.objectToFragmentIndex(pubkeyChannelObject));
            stmt.setBytes(i++, pubkeyChannelObject.getHash());
            stmt.setInt(i++, nodeIdA);
            stmt.setInt(i++, nodeIdB);
            stmt.setBytes(i++, pubkeyChannelObject.secretAHash);
            stmt.setBytes(i++, pubkeyChannelObject.secretBHash);
            stmt.setBytes(i++, pubkeyChannelObject.pubkeyA1);
            stmt.setBytes(i++, pubkeyChannelObject.pubkeyA2);
            stmt.setBytes(i++, pubkeyChannelObject.pubkeyB1);
            stmt.setBytes(i++, pubkeyChannelObject.pubkeyB2);
            stmt.setBytes(i++, pubkeyChannelObject.txidAnchor);
            stmt.setBytes(i++, pubkeyChannelObject.signatureA);
            stmt.setBytes(i++, pubkeyChannelObject.signatureB);

            printInnerStatement((C3P0ProxyStatement) stmt);

            stmt.execute();

            //TODO: Actually return the newly generated id..
            return id;

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static boolean newIPObject (Connection conn, PubkeyIPObject IPObject) throws SQLException {

        PreparedStatement stmt = null;
        try {

            //Check if we have the IP already..
            int id = getIPId(conn, IPObject.pubkey);

            if (id != 0) {

                stmt = conn.prepareStatement("UPDATE pubkey_ips SET fragment_index=?, hash=?, host=?, port=?, timestamp=?, signature=? WHERE id=? AND " + "timestamp<?");
                int i = 1;
                stmt.setInt(i++, TreeMapDatastructure.objectToFragmentIndex(IPObject));
                stmt.setBytes(i++, IPObject.getHash());
                stmt.setString(i++, IPObject.IP);
                stmt.setInt(i++, IPObject.port);
                stmt.setInt(i++, IPObject.timestamp);
                stmt.setBytes(i++, IPObject.signature);
                stmt.setInt(i++, id);
                stmt.setInt(i++, IPObject.timestamp);

                int count = stmt.executeUpdate();

                printInnerStatement((C3P0ProxyStatement) stmt);

                return (count > 0);
            }

            //We don't have the IP currently..

            stmt = conn.prepareStatement("INSERT INTO pubkey_ips VALUES(?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

            int i = 1;
            stmt.setInt(i++, id);
            stmt.setInt(i++, TreeMapDatastructure.objectToFragmentIndex(IPObject));
            stmt.setBytes(i++, IPObject.getHash());
            stmt.setInt(i++, DatabaseHandler.getNodeId(conn, IPObject.pubkey));
            stmt.setString(i++, IPObject.IP);
            stmt.setInt(i++, IPObject.port);
            stmt.setInt(i++, IPObject.timestamp);
            stmt.setBytes(i++, IPObject.signature);

            printInnerStatement((C3P0ProxyStatement) stmt);

            stmt.execute();

            return true;

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static int newChannelStatus (Connection conn, ChannelStatusObject channelStatusObject) throws SQLException {

        PreparedStatement stmt = null;
        try {

            //Lets see if we have the object in the db already
            stmt = conn.prepareStatement("SELECT channel_status.id, channel_status.channel_id FROM channel_status " +
                                             "INNER JOIN channels ON channel_status.channel_id = channels.id " +
                                             "INNER JOIN nodes AS nodes_a_table ON nodes_a_table.id = channels.node_id_a " +
                                             "INNER JOIN nodes AS nodes_b_table ON nodes_b_table.id = channels.node_id_b " +
                                             "WHERE ((nodes_a_table.pubkey = ? AND nodes_b_table.pubkey=?) OR (nodes_a_table.pubkey = ? AND nodes_b_table.pubkey=?))");

            stmt.setBytes(1, channelStatusObject.pubkeyA);
            stmt.setBytes(2, channelStatusObject.pubkeyB);
            stmt.setBytes(3, channelStatusObject.pubkeyB);
            stmt.setBytes(4, channelStatusObject.pubkeyA);

            ResultSet set = stmt.executeQuery();

            int id = 0;

            if (!set.first()) {
                int channelId = getChannelId(conn, channelStatusObject.pubkeyA, channelStatusObject.pubkeyB);
                if (channelId == 0) {
                    //TODO: We don't even know the channel yet. Just drop the object..
                    return 0;
                }

                //Insert a new channel state object into the database as we don't have it there yet
                set.close();
                stmt.close();

                stmt = conn.prepareStatement("INSERT INTO channel_status VALUES(?,?,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
                int i = 1;
                stmt.setInt(i++, id);
                stmt.setInt(i++, TreeMapDatastructure.objectToFragmentIndex(channelStatusObject));
                stmt.setBytes(i++, channelStatusObject.getHash());
                stmt.setInt(i++, channelId);
                stmt.setBytes(i++, channelStatusObject.infoA);
                stmt.setBytes(i++, channelStatusObject.infoB);
                stmt.setInt(i++, channelStatusObject.timestamp);
                stmt.setBytes(i++, channelStatusObject.signatureA);
                stmt.setBytes(i++, channelStatusObject.signatureB);

                printInnerStatement((C3P0ProxyStatement) stmt);

                stmt.execute();

                set = stmt.getGeneratedKeys();
                set.first();
                id = set.getInt(1);
                set.close();
                return id;

            } else {

                //Update the channel set that we have right now in the database

                id = set.getInt("channel_status.id");
                set.close();
                stmt.close();

                stmt = conn.prepareStatement("UPDATE channel_status SET fragment_index=?, hash=?, info_a=?, info_b=?, timestamp=?, signature_a=?, " +
                                                 "signature_b=? " +
                                                 "WHERE" + " id=?");
                int i = 1;
                stmt.setInt(i++, TreeMapDatastructure.objectToFragmentIndex(channelStatusObject));
                stmt.setBytes(i++, channelStatusObject.getHash());
                stmt.setBytes(i++, channelStatusObject.infoA);
                stmt.setBytes(i++, channelStatusObject.infoB);
                stmt.setInt(i++, channelStatusObject.timestamp);
                stmt.setBytes(i++, channelStatusObject.signatureA);
                stmt.setBytes(i++, channelStatusObject.signatureB);
                stmt.setInt(i++, id);

                stmt.execute();

                printInnerStatement((C3P0ProxyStatement) stmt);

                return id;

            }

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static ArrayList<byte[]> checkInv (Connection conn, ArrayList<byte[]> inv) throws SQLException {
        ArrayList<byte[]> arrayList = new ArrayList<>();

        PreparedStatement stmt = null;
        try {
            for (byte[] hash : inv) {

                //Lets see if we have the object in the db already
                stmt = conn.prepareStatement("SELECT hash FROM channels WHERE hash=?");
                stmt.setBytes(1, hash);
                ResultSet set = stmt.executeQuery();
                boolean found = set.first();
                stmt.close();
                set.close();
                if (found) {
                    continue;
                }

                stmt = conn.prepareStatement("SELECT hash FROM channel_status WHERE hash=?");
                stmt.setBytes(1, hash);
                set = stmt.executeQuery();
                found = set.first();
                stmt.close();
                set.close();
                if (found) {
                    continue;
                }

                stmt = conn.prepareStatement("SELECT hash FROM pubkey_ips WHERE hash=?");
                stmt.setBytes(1, hash);
                set = stmt.executeQuery();
                found = set.first();
                stmt.close();
                set.close();
                if (found) {
                    continue;
                }

                arrayList.add(hash);

            }

            return arrayList;

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static ArrayList<DataObject> getDataObjectByHash (Connection conn, ArrayList<byte[]> inv) throws SQLException {
        ArrayList<DataObject> dataList = new ArrayList<>();

        PreparedStatement stmt = null;
        try {
            for (byte[] hash : inv) {

                //Lets see if we have the object in the db already
                stmt = conn.prepareStatement("SELECT * FROM channels " +
                                                 "INNER JOIN nodes AS nodes_a_table ON nodes_a_table.id=channels.node_id_a " +
                                                 "INNER JOIN nodes AS nodes_b_table ON nodes_b_table.id=channels.node_id_b " +
                                                 "WHERE channels.hash=?");

                stmt.setBytes(1, hash);
                ResultSet set = stmt.executeQuery();
                if (set.first()) {
                    PubkeyChannelObject pubkeyChannelObject = new PubkeyChannelObject(set);
                    dataList.add(new DataObject(pubkeyChannelObject));
                    set.close();
                    stmt.close();
                    continue;
                }

                stmt = conn.prepareStatement("SELECT * FROM channel_status " +
                                                 "INNER JOIN channels ON channel_status.channel_id=channels.id " +
                                                 "INNER JOIN nodes AS nodes_a_table ON nodes_a_table.id=channels.node_id_a " +
                                                 "INNER JOIN nodes AS nodes_b_table ON nodes_b_table.id=channels.node_id_b " +
                                                 "WHERE channel_status.hash=?");
                stmt.setBytes(1, hash);
                set = stmt.executeQuery();
                if (set.first()) {
                    ChannelStatusObject pubkeyChannelObject = new ChannelStatusObject(set);
                    dataList.add(new DataObject(pubkeyChannelObject));
                    set.close();
                    stmt.close();
                    continue;
                }

                stmt = conn.prepareStatement("SELECT * FROM pubkey_ips " +
                                                 "INNER JOIN nodes ON nodes.id=pubkey_ips.node_id " +
                                                 "WHERE pubkey_ips.hash=?");
                stmt.setBytes(1, hash);
                set = stmt.executeQuery();
                if (set.first()) {
                    PubkeyIPObject pubkeyChannelObject = new PubkeyIPObject(set);
                    dataList.add(new DataObject(pubkeyChannelObject));
                    set.close();
                    stmt.close();
                }

            }

            return dataList;

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static void fillNodeTableWithRandomData (Connection conn, int entries) throws SQLException {

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO nodes VALUES(?,?)");

            Random random = new Random();
            for (int i = 0; i < entries; i++) {
                byte[] b = new byte[33];
                random.nextBytes(b);
                stmt.setString(1, null);
                stmt.setBytes(2, b);
                stmt.addBatch();
            }
            stmt.executeBatch();

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static void fillChannelStatusTableWithRandomData (Connection conn, int entries) throws SQLException {

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO channel_status VALUES(?,?,?,?,?,?,?)");

            Random random = new Random();
            for (int i = 0; i < entries; i++) {
                byte[] b = new byte[33];
                random.nextBytes(b);
                stmt.setString(1, null);
                stmt.setInt(2, random.nextInt(1000) + 1);
                stmt.setInt(3, i + 1);
                stmt.setInt(4, random.nextInt(200000) + 1);
                stmt.setInt(5, random.nextInt(200000) + 1);
                stmt.setBytes(6, b);
                stmt.setBytes(7, b);
                stmt.addBatch();

            }
            stmt.executeBatch();

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static void fillPubkeyIPTableWithRandomData (Connection conn, int entries) throws SQLException {

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO pubkey_ips VALUES(?,?,?,?,?,?,?)");

            Random random = new Random();
            for (int i = 0; i < entries; i++) {
                byte[] b = new byte[33];
                random.nextBytes(b);
                stmt.setString(1, null);
                stmt.setInt(2, random.nextInt(1000) + 1);
                stmt.setInt(3, i + 1);
                stmt.setString(4, random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255));
                stmt.setInt(5, 8992);
                stmt.setInt(6, Tools.currentTime());
                stmt.setBytes(7, b);
                stmt.addBatch();

            }
            stmt.executeBatch();

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static void fillChannelTableWithRandomData (Connection conn, int entries) throws SQLException {

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO channels VALUES(?,?,?,?,?,?,?,?,?,?,?,?,? )");

            Random random = new Random();
            for (int i = 0; i < entries; i++) {
                byte[] b = new byte[33];
                random.nextBytes(b);
                stmt.setString(1, null);
                stmt.setInt(2, random.nextInt(1000) + 1);
                stmt.setInt(3, random.nextInt(200000) + 1);
                stmt.setInt(4, random.nextInt(200000) + 1);
                stmt.setBytes(5, b);
                stmt.setBytes(6, b);
                stmt.setBytes(7, b);
                stmt.setBytes(8, b);
                stmt.setBytes(9, b);
                stmt.setBytes(10, b);
                stmt.setBytes(11, b);
                stmt.setBytes(12, b);
                stmt.setBytes(13, b);
                stmt.addBatch();
            }
            stmt.executeBatch();

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

    public static ArrayList<DataObject> getSyncDataByFragmentIndex (Connection conn, int index) throws SQLException {
        PreparedStatement stmt = null;
        ArrayList<DataObject> dataObjectList = new ArrayList<>();
        try {
            stmt = conn.prepareStatement("SELECT * FROM channels " +
                                             "INNER JOIN nodes AS nodes_a_table ON nodes_a_table.id=channels.node_id_a " +
                                             "INNER JOIN nodes AS nodes_b_table ON nodes_b_table.id=channels.node_id_b " +
                                             "WHERE channels.fragment_index=?");

            stmt.setInt(1, index);
            ResultSet result = stmt.executeQuery();

            if (result.first()) {
                while (!result.isAfterLast()) {
                    dataObjectList.add(new DataObject(new PubkeyChannelObject(result)));
                    result.next();
                }
            }
            result.close();
            stmt.close();

            stmt = conn.prepareStatement("SELECT * FROM channel_status " +
                                             "INNER JOIN channels ON channel_status.channel_id=channels.id " +
                                             "INNER JOIN nodes AS nodes_a_table ON nodes_a_table.id=channels.node_id_a " +
                                             "INNER JOIN nodes AS nodes_b_table ON nodes_b_table.id=channels.node_id_b " +
                                             "WHERE channel_status.fragment_index=?");

            stmt.setInt(1, index);
            result = stmt.executeQuery();

            if (result.first()) {
                while (!result.isAfterLast()) {
                    dataObjectList.add(new DataObject(new ChannelStatusObject(result)));
                    result.next();
                }
            }
            result.close();
            stmt.close();

            stmt = conn.prepareStatement("SELECT * FROM pubkey_ips " +
                                             "INNER JOIN nodes ON nodes.id=pubkey_ips.node_id " +
                                             "WHERE pubkey_ips.fragment_index=?");

            stmt.setInt(1, index);
            result = stmt.executeQuery();

            if (result.first()) {
                while (!result.isAfterLast()) {
                    dataObjectList.add(new DataObject(new PubkeyIPObject(result)));
                    result.next();
                }
            }

            result.close();

            for(DataObject o : dataObjectList) {
                if(TreeMapDatastructure.objectToFragmentIndex(o.getObject()) != index) {
                    System.out.println("!!!!!!!!!Object should not be in that index.. Is in: "+index+" Should be: "+TreeMapDatastructure.objectToFragmentIndex(o
                                                                                                                                                           .getObject()));

                }
            }

            return dataObjectList;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public static void syncDatalist (Connection conn, ArrayList<P2PDataObject> dataList) throws SQLException {
        for (P2PDataObject object : dataList) {
            if (object instanceof PubkeyChannelObject) {
                DatabaseHandler.newChannel(conn, (PubkeyChannelObject) object);
            }
        }
        for (P2PDataObject object : dataList) {
            if (object instanceof ChannelStatusObject) {
                DatabaseHandler.newChannelStatus(conn, (ChannelStatusObject) object);
            }
        }
        for (P2PDataObject object : dataList) {
            if (object instanceof PubkeyIPObject) {
                DatabaseHandler.newIPObject(conn, (PubkeyIPObject) object);
            }
        }
    }

    public static void newGossipData (Connection conn, DataObject dataObject) throws SQLException {
        P2PDataObject object = dataObject.getObject();
        if (object instanceof PubkeyChannelObject) {
            DatabaseHandler.newChannel(conn, (PubkeyChannelObject) object);
        }
        if (object instanceof ChannelStatusObject) {
            DatabaseHandler.newChannelStatus(conn, (ChannelStatusObject) object);
        }
        if (object instanceof PubkeyIPObject) {
            DatabaseHandler.newIPObject(conn, (PubkeyIPObject) object);
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
