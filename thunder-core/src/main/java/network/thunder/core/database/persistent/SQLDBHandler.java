package network.thunder.core.database.persistent;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.DIRECTION;
import network.thunder.core.communication.layer.MessageWrapper;
import network.thunder.core.communication.layer.high.*;
import network.thunder.core.communication.layer.high.payments.LNOnionHelper;
import network.thunder.core.communication.layer.high.payments.LNOnionHelperImpl;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import network.thunder.core.communication.layer.high.payments.updates.PaymentNew;
import network.thunder.core.communication.layer.high.payments.updates.PaymentRedeem;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.ChannelSettlement;
import network.thunder.core.database.objects.PaymentStatus;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.database.persistent.mapper.*;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.util.ByteArrayColumnMapper;
import org.skife.jdbi.v2.util.IntegerColumnMapper;
import org.skife.jdbi.v2.util.LongColumnMapper;
import org.skife.jdbi.v2.util.StringColumnMapper;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static network.thunder.core.database.objects.PaymentStatus.*;
import static network.thunder.core.database.persistent.DBTableNames.*;

public class SQLDBHandler implements DBHandler {
    private static final Logger log = Tools.getLogger();

    DataSource dataSource;
    DBI dbi;

    LNOnionHelper onionHelper = new LNOnionHelperImpl();

    public static final String SELECT_PAYMENT = "SELECT * FROM " + PAYMENT + " " +
            "INNER JOIN " + ONION + " ON " + ONION + ".hash=" + PAYMENT + ".onion_hash " +
            "INNER JOIN " + SECRET + " ON " + SECRET + ".hash=" + PAYMENT + ".secret_hash " +
            "INNER JOIN " + CHANNEL + " ON " + CHANNEL + ".hash=" + PAYMENT + ".channel_hash " +
            "INNER JOIN " + NODE + " ON " + NODE + ".id=" + CHANNEL + ".node_id ";

    public final static String SELECT_P2P_STATIC =
            "SELECT nodes_a_table.pubkey AS node_a_pubkey, nodes_B_table.pubkey AS node_b_pubkey, " + P2P_CHANNEL_STATIC + ".* " +
                    "FROM " + P2P_CHANNEL_STATIC + " " +
                    "INNER JOIN " + NODE + " AS nodes_a_table ON nodes_a_table.id=" + P2P_CHANNEL_STATIC + ".node_id_a " +
                    "INNER JOIN " + NODE + " AS nodes_b_table ON nodes_b_table.id=" + P2P_CHANNEL_STATIC + ".node_id_b ";

    public final static String SELECT_P2P_DYNAMIC =
            "SELECT nodes_a_table.pubkey AS node_a_pubkey, nodes_B_table.pubkey AS node_b_pubkey, " + P2P_CHANNEL_DYNAMIC + ".* " +
                    "FROM " + P2P_CHANNEL_DYNAMIC + " " +
                    "INNER JOIN " + P2P_CHANNEL_STATIC + " ON " + P2P_CHANNEL_DYNAMIC + ".channel_id=" + P2P_CHANNEL_STATIC + ".id " +
                    "INNER JOIN " + NODE + " AS nodes_a_table ON nodes_a_table.id=" + P2P_CHANNEL_STATIC + ".node_id_a " +
                    "INNER JOIN " + NODE + " AS nodes_b_table ON nodes_b_table.id=" + P2P_CHANNEL_STATIC + ".node_id_b ";

    public final static String SELECT_CHANNEL =
            "SELECT " + CHANNEL + ".*, " + NODE + ".*, " +
                    getRevocationHashColumns("r", "client_current") + ", " +
                    getRevocationHashColumns("r", "client_next") +
                    " FROM " + CHANNEL +
                    " LEFT JOIN " + NODE + " ON " + NODE + ".id=" + CHANNEL + ".node_id" +
                    " LEFT JOIN " + REVO_HASH + " AS r_client_current ON (r_client_current.depth = " + CHANNEL + ".sha_chain_depth AND r_client_current.channel_hash = " + CHANNEL + ".hash)" +
                    " LEFT JOIN " + REVO_HASH + " AS r_client_next ON (r_client_next.depth = (" + CHANNEL + ".sha_chain_depth+1) AND r_client_next.channel_hash = " + CHANNEL + ".hash) ";

    public static String getRevocationHashColumns (String tableName, String prefix) {
        return tableName + "_" + prefix + ".depth AS " + prefix + "_depth," +
                tableName + "_" + prefix + ".hash AS " + prefix + "_hash," +
                tableName + "_" + prefix + ".secret AS " + prefix + "_secret";
    }

    public SQLDBHandler (DataSource dataSource) {
        try {
            this.dataSource = dataSource;
            this.dbi = new DBI(dataSource);

            //Flyway will automatically look in the classpath and go through the migration files.
            //For us these are stored in thunder-core/src/main/resources/db/migrations
            Flyway flyway = new Flyway();
            flyway.setDataSource(dataSource);
            flyway.migrate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getLastBlockHeight () {
        return dbi.open()
                .createQuery("SELECT last_block_height FROM " + METADATA)
                .cleanupHandle()
                .map(IntegerColumnMapper.PRIMITIVE).first();
    }

    @Override
    public void updateLastBlockHeight (int blockHeight) {
        dbi.open()
                .createStatement("UPDATE " + METADATA + " SET last_block_height =:last_block_height")
                .bind("last_block_height", blockHeight)
                .cleanupHandle()
                .execute();
    }

    @Override
    public String getNetwork () {
        return dbi.open()
                .createQuery("SELECT network FROM " + METADATA)
                .cleanupHandle()
                .map(StringColumnMapper.INSTANCE).first();
    }

    @Override
    public void setNetwork (String network) {
        dbi.open()
                .createStatement("UPDATE " + METADATA + " SET network =: network")
                .bind("network", network)
                .cleanupHandle()
                .execute();
    }

    @Override
    public ServerObject getServerObject () {
        byte[] key = dbi.open().createQuery("SELECT server_node_key FROM " + METADATA)
                .cleanupHandle()
                .map(ByteArrayColumnMapper.INSTANCE)
                .first();

        if (key == null) {
            key = new ECKey().getPrivKeyBytes();
            dbi.open().createStatement("UPDATE " + METADATA + " SET server_node_key=:key")
                    .bind("key", key)
                    .cleanupHandle()
                    .execute();
        }

        ServerObject serverObject = new ServerObject();
        serverObject.pubKeyServer = ECKey.fromPrivate(key);

        return serverObject;
    }

    //region P2P Object handling
    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
        List<P2PDataObject> dataObjectList = new ArrayList<>();

        dataObjectList.addAll(
                dbi.open().createQuery(SELECT_P2P_STATIC +
                        "WHERE " + P2P_CHANNEL_STATIC + ".fragment_index=:fragment_index")
                        .bind("fragment_index", fragmentIndex)
                        .cleanupHandle()
                        .map(P2PChannelStaticObjectColumnMapper.INSTANCE)
                        .list()
        );

        dataObjectList.addAll(
                dbi.open().createQuery(SELECT_P2P_DYNAMIC +
                        "WHERE " + P2P_CHANNEL_DYNAMIC + ".fragment_index=:fragment_index")
                        .bind("fragment_index", fragmentIndex)
                        .cleanupHandle()
                        .map(P2PChannelDynamicObjectColumnMapper.INSTANCE)
                        .list()
        );

        dataObjectList.addAll(
                dbi.open().createQuery("SELECT * FROM " + P2P_IP + " " +
                        "INNER JOIN " + NODE + " ON " + NODE + ".id=" + P2P_IP + ".node_id " +
                        "WHERE " + P2P_IP + ".fragment_index=:fragment_index")
                        .bind("fragment_index", fragmentIndex)
                        .cleanupHandle()
                        .map(P2PIPObjectColumnMapper.INSTANCE)
                        .list()
        );
        return dataObjectList;
    }

    @Override
    public void insertIPObjects (List<P2PDataObject> ipList) {

        for (P2PDataObject object : ipList) {
            PubkeyIPObject ipObject = (PubkeyIPObject) object;

            int nodeId = getIdOrInsertNode(ipObject.pubkey);
            insertIPObject(ipObject, nodeId);
        }
    }

    private void insertIPObject (PubkeyIPObject ipObject, Integer nodeId) {

        Handle handle = dbi.open();
        handle.begin();

        Update update = handle.createStatement("INSERT INTO " + P2P_IP + "(" +
                " node_id, fragment_index, hash, host, port, timestamp, signature" +
                ") VALUES (" +
                ":node_id,:fragment_index,:hash,:host,:port,:timestamp,:signature) ON DUPLICATE KEY UPDATE hash=:hash;");

        update.bind("node_id", nodeId);
        P2PIPObjectColumnMapper.bindChannelToQuery(update, ipObject);
        update.execute();

        update = handle.createStatement("DELETE FROM " + P2P_IP +
                " WHERE node_id=:node_id AND timestamp <> (SELECT max(timestamp) " +
                "                              FROM " + P2P_IP + " s2 " +
                "                              WHERE s2.node_id=:node_id)")
                .bind("node_id", nodeId);
        update.execute();
        handle.commit().close();

    }

    private void insertPubkeyChannelObject (PubkeyChannelObject channelObject, int nodeIdA, int nodeIdB) {
        Handle handle = dbi.open();
        handle.begin();
        Update update = handle.createStatement("INSERT INTO " + P2P_CHANNEL_STATIC + "(" +
                " hash, fragment_index, node_id_a, node_id_b, pubkey_a, pubkey_b, txid_anchor, signature_a, signature_b, timestamp" +
                ") VALUES (" +
                ":hash,:fragment_index,:node_id_a,:node_id_b,:pubkey_a,:pubkey_b,:txid_anchor,:signature_a,:signature_b,:timestamp) ON DUPLICATE KEY UPDATE hash=:hash")
                .bind("node_id_a", nodeIdA)
                .bind("node_id_b", nodeIdB);
        P2PChannelStaticObjectColumnMapper.bindChannelToQuery(update, channelObject);
        update.execute();

        //TODO seems to delete more than the ones we want it to delete
//        update = handle.createStatement("DELETE FROM " + P2P_CHANNEL_STATIC +
//                " WHERE (node_id_a=:node_id_a AND node_id_b=:node_id_b)" +
//                "        AND timestamp <> (SELECT max(timestamp) " +
//                "                              FROM " + P2P_CHANNEL_STATIC + " s2 " +
//                "                              WHERE (s2.node_id_a=:node_id_a AND s2.node_id_b=:node_id_b))")
//                .bind("node_id_a", nodeIdA)
//                .bind("node_id_b", nodeIdB);
//
//        update.execute();
        handle.commit().close();

    }

    private void insertChannelStatusObject (ChannelStatusObject statusObject, int channelId) {
        Update update = dbi.open().createStatement("INSERT INTO " + P2P_CHANNEL_DYNAMIC + "(" +
                " hash, fragment_index, channel_id, info_a, info_b, signature_a, signature_b, timestamp" +
                ") VALUES (" +
                ":hash,:fragment_index,:channel_id,:info_a,:info_b,:signature_a,:signature_b,:timestamp) ON DUPLICATE KEY UPDATE hash=:hash")
                .bind("channel_id", channelId)
                .cleanupHandle();

        P2PChannelDynamicObjectColumnMapper.bindChannelToQuery(update, statusObject);


        update.execute();
    }

    private synchronized int getIdOrInsertNode (byte[] nodeKey) {
        Integer nodeId = dbi.open().createQuery("SELECT id FROM " + NODE + " WHERE pubkey = :pubkey")
                .bind("pubkey", nodeKey)
                .cleanupHandle()
                .map(IntegerColumnMapper.PRIMITIVE)
                .first();

        if (nodeId == null) {
            nodeId = insertNode(nodeKey);
        }
        return nodeId;
    }

    private int getChannelObjectId (byte[] nodeKeyA, byte[] nodeKeyB) {
        int nodeIdA = getIdOrInsertNode(nodeKeyA);
        int nodeIdB = getIdOrInsertNode(nodeKeyB);

        Integer id = dbi.open().createQuery("SELECT id FROM " + P2P_CHANNEL_STATIC + " WHERE node_id_a = :node_id_a AND node_id_b = :node_id_b")
                .bind("node_id_a", nodeIdA)
                .bind("node_id_b", nodeIdB)
                .cleanupHandle()
                .map(IntegerColumnMapper.PRIMITIVE)
                .first();

        if (id == null) {
            return 0;
        }

        return id;
    }

    private int insertNode (byte[] nodekey) {
        return dbi.open().createStatement("INSERT INTO " + NODE + "(pubkey) VALUES(:pubkey)")
                .bind("pubkey", nodekey)
                .cleanupHandle()
                .executeAndReturnGeneratedKeys(IntegerColumnMapper.PRIMITIVE)
                .first();
    }

    @Override
    public List<PubkeyIPObject> getIPObjects () {
        return dbi.open().createQuery("SELECT * FROM " + P2P_IP + " " +
                "INNER JOIN " + NODE + " ON " + NODE + ".id=" + P2P_IP + ".node_id ")
                .cleanupHandle()
                .map(P2PIPObjectColumnMapper.INSTANCE)
                .list();
    }

    @Override
    public P2PDataObject getP2PDataObjectByHash (byte[] hash) {
        List<P2PDataObject> dataObjectList = new ArrayList<>();

        dataObjectList.addAll(
                dbi.open().createQuery(SELECT_P2P_STATIC +
                        "WHERE " + P2P_CHANNEL_STATIC + ".hash=:hash")
                        .bind("hash", hash)
                        .cleanupHandle()
                        .map(P2PChannelStaticObjectColumnMapper.INSTANCE)
                        .list()
        );

        dataObjectList.addAll(
                dbi.open().createQuery(SELECT_P2P_DYNAMIC +
                        "WHERE " + P2P_CHANNEL_DYNAMIC + ".hash=:hash")
                        .bind("hash", hash)
                        .cleanupHandle()
                        .map(P2PChannelDynamicObjectColumnMapper.INSTANCE)
                        .list()
        );

        dataObjectList.addAll(
                dbi.open().createQuery("SELECT * FROM " + P2P_IP + " " +
                        "INNER JOIN " + NODE + " ON " + NODE + ".id=" + P2P_IP + ".node_id " +
                        "WHERE " + P2P_IP + ".hash=:hash")
                        .bind("hash", hash)
                        .cleanupHandle()
                        .map(P2PIPObjectColumnMapper.INSTANCE)
                        .list()
        );

        if (dataObjectList.size() == 0) {
            return null;
        } else {
            //TODO when cleanup is implemented
//            if (dataObjectList.size() > 1) {
//                throw new RuntimeException("Got multiple objects with the same hash in our database: " + Tools.bytesToHex(hash));
//            } else {
            return dataObjectList.get(0);
//            }
        }
    }

    @Override
    public List<ChannelStatusObject> getTopology () {
        return dbi.open().createQuery(SELECT_P2P_DYNAMIC)
                .cleanupHandle()
                .map(P2PChannelDynamicObjectColumnMapper.INSTANCE)
                .list();
    }

    @Override
    public PubkeyIPObject getIPObject (byte[] nodeKey) {
        return dbi.open().createQuery("SELECT * FROM " + P2P_IP + " " +
                "INNER JOIN " + NODE + " ON " + NODE + ".id=" + P2P_IP + ".node_id " +
                "WHERE " + NODE + ".pubkey=:nodekey")
                .bind("nodekey", nodeKey)
                .cleanupHandle()
                .map(P2PIPObjectColumnMapper.INSTANCE)
                .first();
    }

    @Override
    public List<PubkeyIPObject> getIPObjectsWithActiveChannel () {
        return dbi.open().createQuery("SELECT nodes.host, nodes.port FROM nodes, channels WHERE channels.is_ready=1 AND node.id=channels.nodeid")
                .cleanupHandle()
                .map(P2PIPObjectColumnMapper.INSTANCE)
                .list();
    }

    @Override
    public void invalidateP2PObject (P2PDataObject ipObject) {
        //TODO
    }

    @Override
    public void syncDatalist (List<P2PDataObject> dataList) {
        for (P2PDataObject dataObject : dataList) {
            if (dataObject instanceof PubkeyIPObject) {
                PubkeyIPObject ipObject = (PubkeyIPObject) dataObject;
                int nodeId = getIdOrInsertNode(ipObject.pubkey);
                insertIPObject(ipObject, nodeId);
            }
            if (dataObject instanceof PubkeyChannelObject) {
                PubkeyChannelObject channelObject = (PubkeyChannelObject) dataObject;
                int nodeIdA = getIdOrInsertNode(channelObject.nodeKeyA);
                int nodeIdB = getIdOrInsertNode(channelObject.nodeKeyB);
                insertPubkeyChannelObject(channelObject, nodeIdA, nodeIdB);
            }
            if (dataObject instanceof ChannelStatusObject) {
                ChannelStatusObject statusObject = (ChannelStatusObject) dataObject;
                int channelId = getChannelObjectId(statusObject.pubkeyA, statusObject.pubkeyB);
                if (channelId != 0) {
                    insertChannelStatusObject(statusObject, channelId);
                } else {
                    log.warn("Tried inserting dynamic channel status without having the channel on file");
                    //TODO..
                }
            }
        }
    }
    //endregion

    @Override
    public List<MessageWrapper> getMessageList (NodeKey nodeKey, Sha256Hash channelHash, String classType) {
        //TODO inner join node table
        List<MessageWrapper> list = dbi.open()
                .createQuery("SELECT * FROM " + MESSAGE + " WHERE node_key = :node_key AND message_class = :message_class" +
                        " ORDER BY id DESC LIMIT 10")
                .bind("node_key", nodeKey.getPubKey())
                .bind("message_class", classType)
                .cleanupHandle()
                .map(MessageRowMapper.INSTANCE).list();

        //Reverse list
        List<MessageWrapper> listReversed = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            listReversed.add(list.get(i));
        }
        return listReversed;
    }

    @Override
    public List<AckableMessage> getUnackedMessageList (NodeKey nodeKey) {
        return dbi.open()
                .createQuery("SELECT * FROM " + MESSAGE + " WHERE node_key = :node_key AND sent = 1 AND acked = 0")
                .bind("node_key", nodeKey.getPubKey())
                .cleanupHandle()
                .map(MessageRowMapper.INSTANCE)
                .list()
                .stream()
                .map(MessageWrapper::getMessage)
                .filter(m -> m instanceof AckableMessage)
                .map(m -> (AckableMessage) m)
                .collect(Collectors.toList());
    }

    @Override
    public NumberedMessage getMessageResponse (NodeKey nodeKey, long messageIdReceived) {
        Handle handle = dbi.open();
        MessageWrapper m = handle
                .createQuery("SELECT * FROM " + MESSAGE + " " +
                        "WHERE node_key = :node_key AND response_to_id = :message_id AND sent=1")
                .bind("node_key", nodeKey.getPubKey())
                .bind("message_id", messageIdReceived)
                .cleanupHandle()
                .map(MessageRowMapper.INSTANCE)
                .first();

        if (m != null) {
            return (NumberedMessage) m.getMessage();
        } else {
            return null;
        }

    }

    @Override
    public void setMessageAcked (NodeKey nodeKey, long messageId) {
        Handle handle = dbi.open();
        setMessageAcked(nodeKey, messageId, handle);
        handle.close();
    }

    private static void setMessageAcked (NodeKey nodeKey, long messageId, Handle handle) {
        handle
                .createStatement("UPDATE " + MESSAGE + " SET acked=1 WHERE node_key = :node_key AND message_id = :message_id AND sent = 1")
                .bind("node_key", nodeKey.getPubKey())
                .bind("message_id", messageId)
                .execute();
    }

    @Override
    public void setMessageProcessed (NodeKey nodeKey, NumberedMessage message) {
        Handle handle = dbi.open();
        setMessageProcessed(nodeKey, message, handle);
        handle.close();
    }

    private static void setMessageProcessed (NodeKey nodeKey, NumberedMessage message, Handle handle) {
        log.debug("Mark message as processed: "+message.getMessageNumber()+" " + message);
        handle
                .createStatement("UPDATE " + MESSAGE + " SET processed=1 WHERE node_key = :node_key AND message_id = :message_id AND sent = 0")
                .bind("node_key", nodeKey.getPubKey())
                .bind("message_id", message.getMessageNumber())
                .execute();
    }

    @Override
    public long lastProcessedMessaged (NodeKey nodeKey) {
        Long l = dbi.open()
                .createQuery("SELECT message_id FROM " + MESSAGE + " WHERE (node_key = :node_key AND processed = 1 AND sent = 0) ORDER BY message_id DESC LIMIT 1")
                .bind("node_key", nodeKey.getPubKey())
                .cleanupHandle()
                .map(LongColumnMapper.WRAPPER)
                .first();
        if (l == null) {
            return 0;
        } else {
            return l.longValue();
        }
    }

    @Override
    public long insertMessage (NodeKey nodeKey, NumberedMessage message, DIRECTION direction) {
        Handle handle = dbi.open();
        long messageId = insertMessage(nodeKey, message, direction, handle);
        handle.close();
        return messageId;
    }

    @NotNull
    private static Long insertMessage (NodeKey nodeKey, NumberedMessage message, DIRECTION direction, Handle handle) {
        //TODO obtain lock from a nodekey map to atomically obtain the new message id
        if(direction == DIRECTION.SENT) {
            Long highestId = handle
                    .createQuery("SELECT message_id FROM " + MESSAGE + " WHERE node_key = :node_key AND sent=1 ORDER BY message_id DESC LIMIT 1")
                    .bind("node_key", nodeKey.getPubKey())
                    .map(LongColumnMapper.WRAPPER)
                    .first();

            if (highestId == null) {
                highestId = 0L;
            }

            message.setMessageNumber(highestId + 1);
        }

        Update query = handle
                .createStatement("INSERT INTO " + MESSAGE + "(" +
                        " message_id, node_key, sent, processed, acked, response_to_id, timestamp, message_class, message_data" +
                        ") VALUES(" +
                        ":message_id,:node_key,:sent,:processed,:acked,:response_to_id,:timestamp,:message_class,:message_data)" +
                        " ON DUPLICATE KEY UPDATE timestamp=:timestamp");

        MessageRowMapper.INSTANCE.bindChannelToQuery(query, new MessageWrapper(message, Tools.currentTime(), direction));
        query
                .bind("node_key", nodeKey.getPubKey())
                .bind("processed", 1)
                .bind("acked", 0)
                .bind("response_to_id", -1)
                .execute();

        handle.createStatement("DELETE FROM "+MESSAGE+" WHERE node_key=:node_key AND sent=:sent AND message_id<:prune_id")
                .bind("node_key", nodeKey.getPubKey())
                .bind("sent", direction==DIRECTION.SENT?1:0)
                .bind("prune_id", message.getMessageNumber() - 20)
                .execute();

        log.debug("Inserted message: directionNumber:{} {} {}", message.getMessageNumber(), direction, message);
        return message.getMessageNumber();
    }

    @Override
    public void linkResponse (NodeKey nodeKey, long messageRequest, long messageResponse) {
        Handle handle = dbi.open();
        linkResponse(nodeKey, messageRequest, messageResponse, handle);
        handle.close();
    }

    private static void linkResponse (NodeKey nodeKey, long messageRequest, long messageResponse, Handle handle) {
        log.debug("Linking " + messageRequest + " to " + messageResponse);

        handle.createStatement("UPDATE " + MESSAGE + " " +
                "SET response_to_id=:request_id " +
                "WHERE node_key = :node_key AND message_id = :response_id")
                .bind("node_key", nodeKey.getPubKey())
                .bind("request_id", messageRequest)
                .bind("response_id", messageResponse)
                .execute();
    }

    @Override
    public Channel getChannel (Sha256Hash hash) {
        Channel channel = dbi.open().createQuery(SELECT_CHANNEL +
                "WHERE " + CHANNEL + ".hash=:hash")
                .bind("hash", hash.getBytes())
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .first();

        channel.paymentList = getPaymentByStatus(channel.getHash(), EMBEDDED);
        return channel;
    }

    @Override
    public List<Channel> getChannel () {
        List<Channel> channels = dbi.open().createQuery(SELECT_CHANNEL)
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .list();
        for (Channel channel : channels) {
            channel.paymentList = getPaymentByStatus(channel.getHash(), EMBEDDED);
        }

        return channels;
    }

    @Override
    public List<Channel> getChannel (NodeKey nodeKey) {
        List<Channel> channels = dbi.open().createQuery(SELECT_CHANNEL +
                "WHERE " + NODE + ".pubkey=:nodekey")
                .bind("nodekey", nodeKey.getPubKey())
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .list();

        for (Channel channel : channels) {
            channel.paymentList = getPaymentByStatus(channel.getHash(), EMBEDDED);
        }
        return channels;
    }

    @Override
    public List<Channel> getOpenChannel (NodeKey nodeKey) {
        Handle handle = dbi.open();
        List<Channel> channels = getOpenChannel(nodeKey, handle);
        handle.close();
        return channels;
    }

    @NotNull
    private static List<Channel> getOpenChannel (NodeKey nodeKey, Handle handle) {
        List<Channel> channels = handle.createQuery(SELECT_CHANNEL +
                "WHERE " + NODE + ".pubkey=:nodekey AND " + CHANNEL + ".phase = 'OPEN'")
                .bind("nodekey", nodeKey.getPubKey())
                .map(ChannelRowMapper.INSTANCE)
                .list();
        for (Channel channel : channels) {
            channel.paymentList = getPaymentByStatus(channel.getHash(), EMBEDDED, handle);
        }
        return channels;
    }

    @Override
    public List<Channel> getOpenChannel () {
        List<Channel> channels = dbi.open().createQuery(SELECT_CHANNEL +
                "WHERE " + CHANNEL + ".phase = 'OPEN'")
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .list();
        for (Channel channel : channels) {
            channel.paymentList = getPaymentByStatus(channel.getHash(), EMBEDDED);
        }

        return channels;
    }

    @Override
    public void insertChannel (Channel channel) {
        int nodeId = getIdOrInsertNode(channel.nodeKeyClient.getPubKey());

        Update query = dbi.open().createStatement("INSERT INTO " + CHANNEL + "(" +
                " hash, node_id, key_client, key_server, address_client, address_server, master_priv_key_client, master_priv_key_server, sha_chain_depth," +
                " amount_server, amount_client, timestamp_open, timestamp_force_close, anchor_tx_hash, anchor_tx_blockheight, anchor_tx_min_conf," +
                " channel_tx_on_chain, channel_tx_signatures, csv_delay, fee_per_byte, phase" +
                ") VALUES (" +
                ":hash,:node_id,:key_client,:key_server,:address_client,:address_server,:master_priv_key_client,:master_priv_key_server,:sha_chain_depth," +
                ":amount_server,:amount_client,:timestamp_open,:timestamp_force_close,:anchor_tx_hash,:anchor_tx_blockheight,:anchor_tx_min_conf," +
                ":channel_tx_on_chain,:channel_tx_signatures,:csv_delay,:fee_per_byte,:phase" +
                ")");
        query.bind("node_id", nodeId);
        ChannelRowMapper.bindChannelToQuery(query, channel);
        query.cleanupHandle().execute();

        insertRevocationHash(channel.getHash(), channel.revoHashClientCurrent);
        insertRevocationHash(channel.getHash(), channel.revoHashClientNext);
    }

    private void insertRevocationHash (Sha256Hash channelHash, RevocationHash revocationHash) {
        Handle handle = dbi.open();
        insertRevocationHash(channelHash, revocationHash, handle);
        handle.close();
    }

    private static void insertRevocationHash (Sha256Hash channelHash, RevocationHash revocationHash, Handle handle) {
        Update update = handle.createStatement("INSERT INTO " + REVO_HASH + "(" +
                " channel_hash, depth, hash, secret" +
                ") VALUES (" +
                ":channel_hash,:depth,:hash,:secret" +
                ") ON DUPLICATE KEY UPDATE hash=hash")
                .bind("channel_hash", channelHash.getBytes());
        RevocationHashRowMapper.bindChannelToQuery(update, revocationHash);
        update.execute();
    }

    @Override
    public void updateChannelStatus (NodeKey nodeKey, @NotNull Sha256Hash channelHash, ECKey keyServer, Channel channel, ChannelUpdate update,
                                     List<RevocationHash> revocationHashList, NumberedMessage request, NumberedMessage response) {
        Handle handle = dbi.open();
        handle.begin();

        if (request != null) {
            insertMessage(nodeKey, request, DIRECTION.RECEIVED, handle);
            setMessageProcessed(nodeKey, request, handle);
        }
        if (response != null) {
            insertMessage(nodeKey, response, DIRECTION.SENT, handle);
            if (request != null) {
                linkResponse(nodeKey, request.getMessageNumber(), response.getMessageNumber(), handle);
            }
            if(response instanceof AckMessage) {
                setMessageAcked(nodeKey, ((AckMessage) response).getMessageNumberToAck(), handle);
            }
        }
        if (revocationHashList != null) {
            for (RevocationHash revocationHash : revocationHashList) {
                insertRevocationHash(channelHash, revocationHash, handle);
            }
        }
        if (channel != null) {
            updateChannel(channel, handle);
        }
        if (update != null) {
            List<PaymentData> currentlyEmbeddingPayments = getPaymentByStatus(channelHash, UNKNOWN, true, handle);

            List<PaymentData> currentPayments = getPaymentByStatus(channelHash, EMBEDDED, handle);

            if (currentlyEmbeddingPayments.size() == 0) {
                for (PaymentNew paymentNew : update.newPayments) {
                    PaymentData paymentSender = new PaymentData(paymentNew, false);
                    paymentSender.status = EMBEDDED;
                    paymentSender.timestampOpen = Tools.currentTime();

                    PaymentData paymentReceiver = new PaymentData(paymentNew, true);

                    PeeledOnion onion = null;
                    onion = onionHelper.loadMessage(keyServer, paymentSender.onionObject);

                    paymentSender.onionObject = onion.onionObject;
                    paymentReceiver.onionObject = onion.onionObject;

                    insertPayment(nodeKey, paymentSender, handle);

                    if (getPaymentSecret(paymentSender.secret, handle).secret != null) {
                        log.trace("SQLDBHandler.updateChannelStatus have the secret");
                        //We have the payment secret, don't do anything here, we will redeem it automatically with the next query for redeemable payments
                    } else if (onion.failedDecrypted) {
                        log.warn("Failed to decrypt the onion object - refund.." + paymentNew);
                    } else if (onion.isLastHop) {
                        log.trace("SQLDBHandler.updateChannelStatus don't have the secret");
                        //This payment was supposed to be for us, but we can't redeem it. Next query for refundable payments will pick it up
                        log.error("Don't have the payment secret - refund..");
                    } else {
                        log.trace("SQLDBHandler.updateChannelStatus relay to next hop");

                        NodeKey nextHop = onion.nextHop;
                        if (getOpenChannel(nextHop, handle).size() > 0) {
                            insertPayment(nextHop, paymentReceiver, handle);
                            linkPayments(paymentSender.paymentId, paymentReceiver.paymentId, handle);
                        } else {
                            log.info("InMemoryDBHandler.updateChannelStatus to be refunded?");
                        }
                    }
                }
            } else {
                for (PaymentData paymentData : currentlyEmbeddingPayments) {
                    paymentData.status = EMBEDDED;
                    paymentData.timestampOpen = Tools.currentTime();
                    updatePayment(paymentData, handle);
                }
            }

            List<PaymentData> redeemedPayments = update.redeemedPayments.stream().map(p -> p.paymentIndex).map(currentPayments::get).collect(Collectors.toList());

            for (PaymentRedeem r : update.redeemedPayments) {
                addPaymentSecret(r.secret, handle);
            }

            for (PaymentData p : redeemedPayments) {
                p.status = REDEEMED;
                updatePayment(p, handle);
            }

            List<PaymentData> refundedPayments = update.refundedPayments.stream().map(p -> p.paymentIndex).map(currentPayments::get).collect(Collectors.toList());

            for (PaymentData p : refundedPayments) {
                p.status = REFUNDED;
                updatePayment(p, handle);
            }

            unlockPayments(channelHash, handle);
        }
        handle.commit();
        handle.close();
    }

    private List<PaymentData> getPaymentByStatus (Sha256Hash channelHash, PaymentStatus status, boolean locked) {
        Handle handle = dbi.open();
        List<PaymentData> payments = getPaymentByStatus(channelHash, status, locked, handle);
        handle.close();
        return payments;
    }

    private static List<PaymentData> getPaymentByStatus (Sha256Hash channelHash, PaymentStatus status, boolean locked, Handle handle) {
        return handle.createQuery(SELECT_PAYMENT + " " +
                "WHERE " + PAYMENT + ".phase=:status AND channel_hash=:channel_hash AND locked=:locked")
                .bind("channel_hash", channelHash.getBytes())
                .bind("status", status.toString())
                .bind("locked", Tools.boolToInt(locked))
                .map(PaymentRowMapper.INSTANCE)
                .list();
    }

    private List<PaymentData> getPaymentByStatus (Sha256Hash channelHash, PaymentStatus status) {
        Handle handle = dbi.open();
        List<PaymentData> payments = getPaymentByStatus(channelHash, status, handle);
        handle.close();
        return payments;
    }

    private static List<PaymentData> getPaymentByStatus (Sha256Hash channelHash, PaymentStatus status, Handle handle) {
        return handle.createQuery(SELECT_PAYMENT + " " +
                    "WHERE " + PAYMENT + ".phase=:status AND channel_hash=:channel_hash ORDER BY timestamp_added ASC")
                    .bind("channel_hash", channelHash.getBytes())
                    .bind("status", status.toString())
                    .map(PaymentRowMapper.INSTANCE)
                    .list();
    }

    @Override
    public void updateChannel (Channel channel) {
        Handle handle = dbi.open();
        updateChannel(channel, handle);
        handle.close();
    }

    private static void updateChannel (Channel channel, Handle handle) {
        Update query = handle.createStatement("UPDATE " + CHANNEL + " SET " +
                "key_client=:key_client, " +
                "key_server=:key_server, " +
                "address_client=:address_client, " +
                "address_server=:address_server, " +
                "master_priv_key_client=:master_priv_key_client, " +
                "master_priv_key_server=:master_priv_key_server, " +
                "sha_chain_depth=:sha_chain_depth, " +
                "amount_server=:amount_server, " +
                "amount_client=:amount_client, " +
                "timestamp_open=:timestamp_open, " +
                "timestamp_force_close=:timestamp_force_close, " +
                "anchor_tx_hash=:anchor_tx_hash, " +
                "anchor_tx_blockheight=:anchor_tx_blockheight, " +
                "anchor_tx_min_conf=:anchor_tx_min_conf, " +
                "channel_tx_on_chain=:channel_tx_on_chain, " +
                "channel_tx_signatures=:channel_tx_signatures, " +
                "csv_delay=:csv_delay, " +
                "fee_per_byte=:fee_per_byte, " +
                "phase=:phase" +
                " WHERE hash=:hash")
                .bind("hash", channel.getHash());
        ChannelRowMapper
                .bindChannelToQuery(query, channel)
                .execute();

        insertRevocationHash(channel.getHash(), channel.revoHashClientCurrent, handle);
        insertRevocationHash(channel.getHash(), channel.revoHashClientNext, handle);
    }

    @Override
    public RevocationHash retrieveRevocationHash (Sha256Hash channelHash, int shaChainDepth) {
        return dbi.open().createQuery("SELECT * FROM " + REVO_HASH + " WHERE cannel_hash=:channel_hash AND depth=:depth")
                .bind("channel_hash", channelHash.getBytes())
                .bind("depth", shaChainDepth)
                .map(RevocationHashRowMapper.INSTANCE)
                .first();
    }

    @Override
    public List<PaymentData> lockPaymentsToBeRefunded (NodeKey nodeKey) {
        //TODO also check for payments that aren't embedded on the other side but didn't got refunded either somehow...
        Handle handle = dbi.open();
        handle.begin();

        List<PaymentData> paymentList = handle.createQuery(SELECT_PAYMENT +

                "LEFT JOIN " + PAYMENT_LINKS + " ON " + PAYMENT_LINKS + ".sender=" + PAYMENT + ".id " +
                "LEFT JOIN " + PAYMENT + " AS payment_other ON " + PAYMENT_LINKS + ".receiver=payment_other.id " +

                "WHERE " +
                "((payment_other.phase=:other_payment_status) " +
                "OR" +
                "(" + PAYMENT_LINKS + ".receiver IS NULL))" +
                " AND " + SECRET + ".secret IS NOT NULL" +
                " AND " + PAYMENT + ".phase=:this_payment_status" +
                " AND " + PAYMENT + ".sending=0 " +
                " AND " + PAYMENT + ".locked=0 " +
                " AND " + NODE + ".pubkey=:nodekey")
                .bind("other_payment_status", "REFUNDED")
                .bind("this_payment_status", "EMBEDDED")
                .bind("nodekey", nodeKey.getPubKey())
                .map(PaymentRowMapper.INSTANCE)
                .list();

        for (PaymentData paymentData : paymentList) {
            log.trace("SQLDBHandler.lockPaymentsToBeRefunded lock " + paymentData.paymentId);
            handle.createStatement("UPDATE " + PAYMENT + " SET " +
                    "locked=1" +
                    " WHERE id=:id")
                    .bind("id", paymentData.paymentId)
                    .execute();
        }
        paymentList.forEach(p -> p.status = CURRENTLY_REFUNDING);
        handle.commit();
        handle.close();
        return paymentList;
    }

    @Override
    public List<PaymentData> lockPaymentsToBeRedeemed (NodeKey nodeKey) {
        Handle handle = dbi.open();
        handle.begin();

        List<PaymentData> paymentList = handle.createQuery(SELECT_PAYMENT +
                "WHERE " +
                SECRET + ".secret IS NOT NULL " +
                " AND " + PAYMENT + ".sending=0" +
                " AND " + PAYMENT + ".phase=:this_payment_status" +
                " AND " + PAYMENT + ".locked=0 " +
                " AND " + NODE + ".pubkey=:nodekey")
                .bind("this_payment_status", "EMBEDDED")
                .bind("nodekey", nodeKey.getPubKey())
                .map(PaymentRowMapper.INSTANCE)
                .list();

        for (PaymentData paymentData : paymentList) {
            log.trace("SQLDBHandler.lockPaymentsToBeRedeemed lock " + paymentData.paymentId + " " + paymentData);
            handle.createStatement("UPDATE " + PAYMENT + " SET " +
                    "locked=1" +
                    " WHERE id=:id")
                    .bind("id", paymentData.paymentId)
                    .execute();
        }
        paymentList.forEach(p -> p.status = CURRENTLY_REDEEMING);
        handle.commit();
        handle.close();
        return paymentList;
    }

    @Override
    public List<PaymentData> lockPaymentsToBeMade (NodeKey nodeKey) {
        Handle handle = dbi.open();
        handle.begin();

        List<PaymentData> paymentList = handle.createQuery(SELECT_PAYMENT +

                "LEFT JOIN " + PAYMENT_LINKS + " ON " + PAYMENT_LINKS + ".receiver=" + PAYMENT + ".id " +
                "LEFT JOIN " + PAYMENT + " AS payment_other ON " + PAYMENT_LINKS + ".sender=payment_other.id " +

                "WHERE " +
                "((payment_other.phase=:other_payment_status AND " + PAYMENT + ".phase=:this_payment_status)" +
                "OR" +
                "(" + PAYMENT_LINKS + ".sender IS NULL AND " + PAYMENT + ".phase=:this_payment_status)) " +
                "AND " + PAYMENT + ".locked=0 " +
                "AND " + NODE + ".pubkey=:nodekey")
                .bind("other_payment_status", "EMBEDDED")
                .bind("this_payment_status", "UNKNOWN")
                .bind("nodekey", nodeKey.getPubKey())
                .map(PaymentRowMapper.INSTANCE)
                .list();

        for (PaymentData paymentData : paymentList) {
            log.trace("SQLDBHandler.lockPaymentsToBeMade lock " + paymentData.paymentId);
            handle.createStatement("UPDATE " + PAYMENT + " SET " +
                    "locked=1" +
                    " WHERE id=:id")
                    .bind("id", paymentData.paymentId)
                    .execute();
        }
        paymentList.forEach(p -> p.status = CURRENTLY_EMBEDDING);
        handle.commit();
        handle.close();

        return paymentList;
    }

    @Override
    public void unlockPayments (Sha256Hash channelHash) {
        Handle handle = dbi.open();
        unlockPayments(channelHash, handle);
        handle.close();
    }

    private static void unlockPayments (Sha256Hash channelHash, Handle handle) {
        handle
                .createStatement("UPDATE " + PAYMENT + " SET locked=0 WHERE channel_hash=:channel_hash")
                .bind("channel_hash", channelHash.getBytes())
                .execute();
    }

    private static void linkPayments (int senderPaymentId, int receiverPaymentId, Handle handle) {
        handle.createStatement("INSERT INTO " + PAYMENT_LINKS + "(sender,receiver) VALUES (:sender,:receiver)")
                .bind("sender", senderPaymentId)
                .bind("receiver", receiverPaymentId)
                .execute();
    }

    @Override
    public NodeKey getSenderOfPayment (PaymentSecret paymentSecret) {
        return null; //TODO
    }

    @Override
    public int insertPayment (NodeKey node, PaymentData paymentData) {
        Handle handle = dbi.open();
        int id = insertPayment(node, paymentData, handle);
        handle.close();
        return id;
    }

    private static int insertPayment (NodeKey node, PaymentData paymentData, Handle handle) {
        addPaymentSecret(paymentData.secret, handle);
        addOnionObject(paymentData.onionObject, handle);
        Sha256Hash channelHash = getOpenChannel(node, handle).get(0).getHash(); //TODO error handling, multiple channels, ...

        Update update = handle.createStatement("INSERT INTO " + PAYMENT + " (" +
                " channel_hash, sending, amount, phase, locked, secret_hash, onion_hash, timestamp_added, timestamp_refund, timestamp_settled, version_added, version_settled " +
                ") VALUES (" +
                ":channel_hash,:sending,:amount,:phase,:locked,:secret_hash,:onion_hash,:timestamp_added,:timestamp_refund,:timestamp_settled,:version_added,:version_settled " +
                ")");

        int id = PaymentRowMapper.bindChannelToQuery(update, paymentData)
                .bind("onion_id", Tools.hashSecret(paymentData.onionObject.data))
                .bind("channel_hash", channelHash.getBytes())
                .bind("locked", 0)
                .executeAndReturnGeneratedKeys(IntegerColumnMapper.PRIMITIVE)
                .first();

        paymentData.paymentId = id;
        log.trace("SQLDBHandler.insertPayment " + id + "=" + paymentData);
        return id;
    }

    @Override
    public void updatePayment (PaymentData paymentData) {
        Handle handle = dbi.open();
        updatePayment(paymentData, handle);
        handle.close();
    }

    private static void updatePayment (PaymentData paymentData, Handle handle) {
        addPaymentSecret(paymentData.secret, handle);
        addOnionObject(paymentData.onionObject, handle);
        Update update = handle.createStatement("UPDATE " + PAYMENT + " SET " +
                "sending=:sending, " +
                "amount=:amount, " +
                "phase=:phase," +
                "secret_hash=:secret_hash, " +
                "onion_hash=:onion_hash, " +
                "timestamp_added=:timestamp_added, " +
                "timestamp_refund=:timestamp_refund, " +
                "timestamp_settled=:timestamp_settled, " +
                "version_added=:version_added, " +
                "version_settled=:version_settled " +
                "WHERE id=:payment_id");
        PaymentRowMapper.bindChannelToQuery(update, paymentData)
                .bind("payment_id", paymentData.paymentId)
                .execute();
        log.trace("SQLDBHandler.updatePayment " + paymentData);
    }

    @Override
    public PaymentData getPayment (int paymentId) {
        return dbi.open().createQuery(SELECT_PAYMENT + " " +
                "WHERE " + PAYMENT + ".id=:payment_id")
                .bind("payment_id", paymentId)
                .cleanupHandle()
                .map(PaymentRowMapper.INSTANCE)
                .first();
    }

    @Override
    public PaymentSecret getPaymentSecret (PaymentSecret secret) {
        Handle handle = dbi.open();
        PaymentSecret paymentSecret = getPaymentSecret(secret, handle);
        handle.close();
        return paymentSecret;
    }

    private static PaymentSecret getPaymentSecret (PaymentSecret secret, Handle handle) {
        return handle.createQuery("SELECT * FROM " + SECRET + " WHERE hash=:hash")
                    .bind("hash", secret.hash)
                    .map(PaymentSecretRowMapper.INSTANCE)
                    .first();
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {
        Handle handle = dbi.open();
        addPaymentSecret(secret, handle);
        handle.close();
    }

    private static void addPaymentSecret (PaymentSecret secret, Handle handle) {
        if (secret.secret == null) {
            handle.createStatement("INSERT INTO " + SECRET + " (hash, secret) VALUES (:hash,:secret) ON DUPLICATE KEY UPDATE hash=:hash")
                    .bind("hash", secret.hash)
                    .bind("secret", secret.secret)
                    .execute();
        } else {
            log.trace("Added payment secret with secret.. " + secret);
            handle.createStatement("INSERT INTO " + SECRET + " (hash, secret) VALUES (:hash,:secret) ON DUPLICATE KEY UPDATE secret=:secret")
                    .bind("hash", secret.hash)
                    .bind("secret", secret.secret)
                    .execute();
        }
    }

    public void addOnionObject (OnionObject onionObject) {
        Handle handle = dbi.open();
        addOnionObject(onionObject, handle);
        handle.close();
    }

    private static void addOnionObject (OnionObject onionObject, Handle handle) {
        handle.createStatement("INSERT INTO " + ONION + " (hash, data) VALUES (:hash,:data) ON DUPLICATE KEY UPDATE hash=:hash") //Don't overwrite the data
                .bind("hash", Tools.hashSecret(onionObject.data))
                .bind("data", onionObject.data)
                .execute();
    }

    @Override
    public List<PaymentWrapper> getAllPayments () {
        return new ArrayList<>(); //TODO
    }

    @Override
    public List<PaymentData> getAllPayments (Sha256Hash channelHash) {
        List<PaymentData> paymentDatas = dbi.open().createQuery(SELECT_PAYMENT + " " +
                " WHERE " + PAYMENT + ".channel_hash=:channel_hash ORDER BY timestamp_added ASC")
                .bind("channel_hash", channelHash.getBytes())
                .cleanupHandle()
                .map(PaymentRowMapper.INSTANCE)
                .list();
        return paymentDatas;
    }

    @Override
    public List<PaymentWrapper> getOpenPayments () {
        return new ArrayList<>(); //TODO
    }

    @Override
    public List<PaymentWrapper> getRefundedPayments () {
        return new ArrayList<>(); //TODO
    }

    @Override
    public List<PaymentWrapper> getRedeemedPayments () {
        return new ArrayList<>(); //TODO
    }

    @Override
    public List<ChannelSettlement> getSettlements (Sha256Hash channelHash) {
        return dbi.open().createQuery("SELECT FROM " + SETTLEMENT + " " +
                "INNER JOIN " + REVO_HASH + " ON " + REVO_HASH + ".hash = " + SETTLEMENT + ".revocation_hash " +
                "INNER JOIN " + PAYMENT + " ON " + PAYMENT + ".id = " + SETTLEMENT + ".payment_id " +
                "INNER JOIN " + ONION + " ON " + ONION + ".id=" + PAYMENT + ".onion_id " +
                "INNER JOIN " + SECRET + " ON " + SECRET + ".id=" + PAYMENT + ".secret_id " +
                "WHERE " + SETTLEMENT + ".channel_hash=:channel_hash")
                .bind("channel_hash", channelHash.getBytes())
                .cleanupHandle()
                .map(SettlementRowMapper.INSTANCE)
                .list();
    }

    @Override
    public void addPaymentSettlement (ChannelSettlement settlement) {
        Update update = dbi.open().createStatement("INSERT INTO " + SETTLEMENT + "(" +
                "  channel_hash, phase, timestamp_to_settle, our_channel_tx, cheated, is_payment, revocation_hash, payment_id, channel_tx, second_tx, third_tx, channel_tx_height" +
                ", second_tx_height, third_tx_height, channel_tx_output, second_tx_output, third_tx_output" +
                ") VALUES (" +
                " :channel_hash,:phase,:timestamp_to_settle,:our_channel_tx,:cheated,:is_payment,:revocation_hash,:payment_id,:channel_tx,:second_tx,:third_tx,:channel_tx_height" +
                ",:second_tx_height,:third_tx_height,:channel_tx_output,:second_tx_output,:third_tx_output");

        SettlementRowMapper.bindChannelToQuery(update, settlement)
                .cleanupHandle()
                .execute();
    }

    @Override
    public void updatePaymentSettlement (ChannelSettlement settlement) {
        Update update = dbi.open().createStatement("UPDATE " + SETTLEMENT + " SET " +
                "channel_hash=:channel_hash," +
                "phase=:phase," +
                "timestamp_to_settle=:timestamp_to_settle," +
                "our_channel_tx=:our_channel_tx," +
                "cheated=:cheated," +
                "is_payment=:is_payment," +
                "revocation_hash=:revocation_hash," +
                "payment_id=:payment_id," +
                "channel_tx=:channel_tx," +
                "second_tx=:second_tx," +
                "third_tx=:third_tx," +
                "channel_tx_height=:channel_tx_height," +
                "second_tx_height=:second_tx_height," +
                "third_tx_height=:third_tx_height," +
                "channel_tx_output=:channel_tx_output," +
                "second_tx_output=:second_tx_output," +
                "third_tx_output=:third_tx_output" +
                " WHERE id=:settlement_id");

        SettlementRowMapper.bindChannelToQuery(update, settlement)
                .bind("settlement_id", settlement.settlementId)
                .cleanupHandle()
                .execute();

    }
}
