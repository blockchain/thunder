package network.thunder.core.database.persistent;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.DIRECTION;
import network.thunder.core.communication.layer.MessageWrapper;
import network.thunder.core.communication.layer.high.AckableMessage;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.NumberedMessage;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.ChannelSettlement;
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
import org.skife.jdbi.v2.util.IntegerColumnMapper;
import org.skife.jdbi.v2.util.LongColumnMapper;
import org.skife.jdbi.v2.util.StringColumnMapper;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static network.thunder.core.database.persistent.DBTableNames.*;

public class SQLDBHandler implements DBHandler {
    DataSource dataSource;
    DBI dbi;

    public final static String SELECT_P2P_STATIC =
            "SELECT nodes_a_table.pubkey AS node_a_pubkey, nodes_B_table.pubkey AS node_b_pubkey, " + P2P_CHANNEL_STATIC + ".* " +
                    "FROM " + P2P_CHANNEL_STATIC + " " +
                    "INNER JOIN " + NODE + " AS nodes_a_table ON nodes_a_table.id=" + P2P_CHANNEL_STATIC + ".node_id_a " +
                    "INNER JOIN " + NODE + " AS nodes_b_table ON nodes_b_table.id=" + P2P_CHANNEL_STATIC + ".node_id_b ";

    public final static String SELECT_P2P_DYNAMIC =
            "SELECT  nodes_a_table.pubkey AS node_a_pubkey, nodes_B_table.pubkey AS node_b_pubkey, " + P2P_CHANNEL_DYNAMIC + ".* " +
                    "FROM " + P2P_CHANNEL_DYNAMIC + " " +
                    "INNER JOIN " + P2P_CHANNEL_STATIC + " ON " + P2P_CHANNEL_DYNAMIC + ".channel_id=" + P2P_CHANNEL_STATIC + ".id " +
                    "INNER JOIN " + NODE + " AS nodes_a_table ON nodes_a_table.id=" + P2P_CHANNEL_STATIC + ".node_id_a " +
                    "INNER JOIN " + NODE + " AS nodes_b_table ON nodes_b_table.id=" + P2P_CHANNEL_STATIC + ".node_id_b ";

    public final static String SELECT_CHANNEL =
            "SELECT " + CHANNEL + ".*, " + NODE + ".*, " +
                    getRevocationHashColumns("r", "client_current") + ", " +
                    getRevocationHashColumns("r", "client_next") +
                    " FROM " + CHANNEL +
                    " INNER JOIN " + NODE + " ON " + NODE + ".id=" + CHANNEL + ".node_id" +
                    " INNER JOIN " + REVO_HASH + " AS r_client_current ON (r_client_current.depth = channel.sha_chain_depth AND r.channel_hash = channel.hash)" +
                    " INNER JOIN " + REVO_HASH + " AS r_client_next ON (r_client_next.depth = (channel.sha_chain_depth+1) AND r.channel_hash = channel.hash) ";

    public static String getRevocationHashColumns (String tableName, String prefix) {
        return tableName + "_" + prefix + ".depth AS " + prefix + "_depth," +
                tableName + "_" + prefix + ".hash AS " + prefix + "_hash," +
                tableName + "_" + prefix + ".secret AS " + prefix + "_depth";
    }

    public SQLDBHandler (DataSource dataSource) {
        try {
            this.dataSource = dataSource;
            this.dbi = new DBI(dataSource);

            Handle h = dbi.open();

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
    public List<MessageWrapper> getMessageList (NodeKey nodeKey, Sha256Hash channelHash, Class c) {
        return dbi.open()
                .createQuery("SELECT * FROM " + MESSAGE + " WHERE channel_hash = :channel_hash AND message_class = :message_class")
                .bind("channel_hash", channelHash.getBytes())
                .bind("message_class", c.toString())
                .cleanupHandle()
                .map(MessageRowMapper.INSTANCE).list();
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
        return (NumberedMessage) dbi.open()
                .createQuery("SELECT * FROM " + MESSAGE + " WHERE node_key = :node_key AND message_id = :message_id")
                .bind("node_key", nodeKey.getPubKey())
                .bind("message_id", messageIdReceived)
                .cleanupHandle()
                .map(MessageRowMapper.INSTANCE)
                .first()
                .getMessage();
    }

    @Override
    public void setMessageAcked (NodeKey nodeKey, long messageId) {
        dbi.open()
                .createStatement("UPDATE " + MESSAGE + " SET (acked=1) WHERE node_key = :node_key AND message_id = :message_id")
                .bind("node_key", nodeKey.getPubKey())
                .bind("message_id", messageId)
                .cleanupHandle()
                .execute();
    }

    @Override
    public void setMessageProcessed (NodeKey nodeKey, NumberedMessage message) {
        dbi.open()
                .createStatement("UPDATE " + MESSAGE + " SET (processed=1) WHERE node_key = :node_key AND message_id = :message_id")
                .bind("node_key", nodeKey.getPubKey())
                .bind("message_id", message.getMessageNumber())
                .cleanupHandle()
                .execute();
    }

    @Override
    public long lastProcessedMessaged (NodeKey nodeKey) {
        return dbi.open()
                .createQuery("SELECT message_id FROM " + MESSAGE + " WHERE (node_key = :node_key AND processed = 1) ORDER BY message_id DESC LIMIT 1")
                .bind("node_key", nodeKey.getPubKey())
                .cleanupHandle()
                .map(LongColumnMapper.PRIMITIVE)
                .first();
    }

    @Override
    public long saveMessage (NodeKey nodeKey, NumberedMessage message, DIRECTION direction) {
        //TODO obtain lock from a nodekey map to atomically obtain the new message id
        long highestId = dbi.open()
                .createQuery("SELECT message_id FROM " + MESSAGE + " WHERE node_key = :node_key ORDER BY message_id DESC LIMIT 1")
                .bind("node_key", nodeKey.getPubKey())
                .cleanupHandle()
                .map(LongColumnMapper.PRIMITIVE)
                .first();

        message.setMessageNumber(highestId + 1);

        Update query = dbi.open()
                .createStatement("INSERT INTO " + MESSAGE + "(" +
                        "message_id," +
                        "node_key," +
                        "sent" +
                        "processed," +
                        "acked," +
                        "response_message_id," +
                        "timestamp," +
                        "message_class," +
                        "message_data" +
                        ") VALUES(" +
                        ":message_id," +
                        ":node_key," +
                        ":sent," +
                        ":processed," +
                        ":acked," +
                        ":response_message_id," +
                        ":timestamp," +
                        ":message_class," +
                        ":message_data)");

        MessageRowMapper.INSTANCE.bindChannelToQuery(query, new MessageWrapper(message, 0, direction));
        query
                .cleanupHandle()
                .execute();

        return highestId + 1;
    }

    @Override
    public void linkResponse (NodeKey nodeKey, long messageRequest, long messageResponse) {
        dbi.open()
                .createStatement("UPDATE " + MESSAGE + " " +
                        "SET (response_message_id=:response_message_id) " +
                        "WHERE node_key = :node_key AND message_id = :message_id")
                .bind("node_key", nodeKey.getPubKey())
                .bind("message_id", messageRequest)
                .bind("response_message_id", messageResponse)
                .cleanupHandle()
                .execute();
    }

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
        Update update = dbi.open().createStatement("INSERT INTO " + P2P_IP + "(" +
                " node_id, fragment_index, hash, host, port, timestamp, signature" +
                ") VALUES (" +
                ":node_id,:fragment_index,:hash,:host,:port,:timestamp,:signature);")
                .cleanupHandle();

        update.bind("node_id", nodeId);
        P2PIPObjectColumnMapper.bindChannelToQuery(update, ipObject);
        update.execute();
    }

    private void insertPubkeyChannelObject (PubkeyChannelObject channelObject, int nodeIdA, int nodeIdB) {
        Update update = dbi.open().createStatement("INSERT INTO " + P2P_CHANNEL_STATIC + "(" +
                " hash, fragment_index, node_id_a, node_id_b, pubkey_a, pubkey_b, txid_anchor, signature_a, signature_b, timestamp" +
                ") VALUES (" +
                ":hash,:fragment_index,:node_id_a,:node_id_b,:pubkey_a,:pubkey_b,:txid_anchor,:signature_a,:signature_b,:timestamp)")
                .bind("node_id_a", nodeIdA)
                .bind("node_id_b", nodeIdB)
                .cleanupHandle();

        P2PChannelStaticObjectColumnMapper.bindChannelToQuery(update, channelObject);
        update.execute();
    }

    private void insertChannelStatusObject (ChannelStatusObject statusObject, int channelId) {
        Update update = dbi.open().createStatement("INSERT INTO " + P2P_CHANNEL_DYNAMIC + "(" +
                " hash, fragment_index, channel_id, info_a, info_b, signature_a, signature_b, timestamp" +
                ") VALUES (" +
                ":hash,:fragment_index,:channel_id,:info_a,:info_b,:signature_a,:signature_b,:timestamp)")
                .bind("channel_id", channelId)
                .cleanupHandle();

        P2PChannelDynamicObjectColumnMapper.bindChannelToQuery(update, statusObject);
        update.execute();
    }

    private int getIdOrInsertNode (byte[] nodeKey) {
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

        Integer id = dbi.open().createQuery("SELECT id FROM " + P2P_CHANNEL_STATIC + " WHERE node_key_a = :node_id_a AND node_id_b = :node_id_b")
                .bind("node_id_a", nodeIdA)
                .bind("node_id_b", nodeIdB)
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
        return dbi.open().createQuery("SELECT * FROM " + NODE + " WHERE")
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
            if (dataObjectList.size() > 1) {
                throw new RuntimeException("Got multiple objects with the same hash in our database: " + Tools.bytesToHex(hash));
            } else {
                return dataObjectList.get(0);
            }
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
        return dbi.open().createQuery("SELECT * FROM " + P2P_CHANNEL_STATIC +
                "INNER JOIN " + NODE + " AS nodes_a_table ON nodes_a_table.id=" + P2P_CHANNEL_STATIC + ".node_id_a " +
                "INNER JOIN " + NODE + " AS nodes_b_table ON nodes_b_table.id=" + P2P_CHANNEL_STATIC + ".node_id_b " +
                "WHERE " + NODE + ".pubkey=:pubkey")
                .bind("pubkey", nodeKey)
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
                    //TODO..
                }
            }
        }
    }

    @Override
    public Channel getChannel (Sha256Hash hash) {
        return dbi.open().createQuery(SELECT_CHANNEL +
                "WHERE " + CHANNEL + ".hash=:hash")
                .bind("hash", hash.getBytes())
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .first();
    }

    @Override
    public List<Channel> getChannel () {
        return dbi.open().createQuery(SELECT_CHANNEL)
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .list();
    }

    @Override
    public List<Channel> getChannel (NodeKey nodeKey) {
        return dbi.open().createQuery(SELECT_CHANNEL +
                "WHERE " + NODE + ".pubkey=:nodekey")
                .bind("nodekey", nodeKey.getPubKey())
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .list();
    }

    @Override
    public List<Channel> getOpenChannel (NodeKey nodeKey) {
        return dbi.open().createQuery(SELECT_CHANNEL +
                "WHERE " + NODE + ".pubkey=:nodekey AND " + CHANNEL + ".phase = 'OPEN'")
                .bind("nodekey", nodeKey.getPubKey())
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .list();
    }

    @Override
    public List<Channel> getOpenChannel () {
        return dbi.open().createQuery(SELECT_CHANNEL +
                "WHERE " + CHANNEL + ".phase = 'OPEN'")
                .cleanupHandle()
                .map(ChannelRowMapper.INSTANCE)
                .list();
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
        Update update = dbi.open().createStatement("INSERT INTO " + REVO_HASH + "(" +
                " channel_hash, depth, hash, secret" +
                ") VALUES (" +
                ":channel_hash,:depth,:hash,:secret" +
                ") ON DUPLICATE KEY UPDATE hash=hash")
                .bind("channel_hash", channelHash.getBytes());
        RevocationHashRowMapper.bindChannelToQuery(update, revocationHash);
        update.cleanupHandle().execute();
    }

    @Override
    public void updateChannelStatus (NodeKey nodeKey, @NotNull Sha256Hash channelHash, ECKey keyServer, Channel channel, ChannelUpdate update,
                                     List<RevocationHash> revocationHash, NumberedMessage request, NumberedMessage response) {
//TODO
    }

    @Override
    public void updateChannel (Channel channel) {

        Update query = dbi.open().createStatement("UPDATE " + CHANNEL + " SET " +
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
        ChannelRowMapper.bindChannelToQuery(query, channel);
        query.cleanupHandle().execute();

        insertRevocationHash(channel.getHash(), channel.revoHashClientCurrent);
        insertRevocationHash(channel.getHash(), channel.revoHashClientNext);
    }

    @Override
    public RevocationHash retrieveRevocationHash (Sha256Hash channelHash, int shaChainDepth) {
        return dbi.open().createQuery("SELECT * FROM " + DBTableNames.REVO_HASH + " WHERE cannel_hash=:channel_hash AND depth=:depth")
                .bind("channel_hash", channelHash.getBytes())
                .bind("depth", shaChainDepth)
                .map(RevocationHashRowMapper.INSTANCE)
                .first();
    }

    @Override
    public List<PaymentData> lockPaymentsToBeRefunded (NodeKey nodeKey) {
        return null;
    }

    @Override
    public List<PaymentData> lockPaymentsToBeMade (NodeKey nodeKey) {
        return null;
    }

    @Override
    public List<PaymentData> lockPaymentsToBeRedeemed (NodeKey nodeKey) {
        return null;
    }

    @Override
    public void checkPaymentsList () {

    }

    @Override
    public void unlockPayments (NodeKey nodeKey, List<PaymentData> paymentList) {

    }

    @Override
    public NodeKey getSenderOfPayment (PaymentSecret paymentSecret) {
        return null;
    }

    @Override
    public void addPayment (NodeKey firstHop, PaymentData paymentWrapper) {

    }

    @Override
    public void updatePayment (PaymentWrapper paymentWrapper) {

    }

    @Override
    public PaymentWrapper getPayment (PaymentSecret paymentSecret) {
        return null;
    }

    @Override
    public PaymentSecret getPaymentSecret (PaymentSecret secret) {
        return null;
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {

    }

    @Override
    public List<ChannelSettlement> getSettlements (Sha256Hash channelHash) {
        return null;
    }

    @Override
    public void addPaymentSettlement (ChannelSettlement settlement) {

    }

    @Override
    public void updatePaymentSettlement (ChannelSettlement settlement) {

    }

    @Override
    public List<PaymentWrapper> getAllPayments () {
        return null;
    }

    @Override
    public List<PaymentData> getAllPayments (Sha256Hash channelHash) {
        return null;
    }

    @Override
    public List<PaymentWrapper> getOpenPayments () {
        return null;
    }

    @Override
    public List<PaymentWrapper> getRefundedPayments () {
        return null;
    }

    @Override
    public List<PaymentWrapper> getRedeemedPayments () {
        return null;
    }
}
