package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.database.persistent.DBTableNames;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class P2PChannelStaticObjectColumnMapper implements ResultSetMapper<PubkeyChannelObject> {
    public static final P2PChannelStaticObjectColumnMapper INSTANCE = new P2PChannelStaticObjectColumnMapper();

    @Override
    public PubkeyChannelObject map (int index, ResultSet set, StatementContext ctx) throws SQLException {
        PubkeyChannelObject channelObject = new PubkeyChannelObject();

        channelObject.channelKeyA = set.getBytes(DBTableNames.P2P_CHANNEL_STATIC+".pubkey_a");
        channelObject.channelKeyB = set.getBytes(DBTableNames.P2P_CHANNEL_STATIC+".pubkey_b");

        channelObject.txidAnchor = set.getBytes(DBTableNames.P2P_CHANNEL_STATIC+".txid_anchor");
        channelObject.signatureA = set.getBytes(DBTableNames.P2P_CHANNEL_STATIC+".signature_a");
        channelObject.signatureB = set.getBytes(DBTableNames.P2P_CHANNEL_STATIC+".signature_b");

        channelObject.timestamp = set.getInt(DBTableNames.P2P_CHANNEL_STATIC+".timestamp");

        channelObject.nodeKeyA = set.getBytes("node_a_pubkey");
        channelObject.nodeKeyB = set.getBytes("node_b_pubkey");

        return channelObject;
    }

    public static void bindChannelToQuery (SQLStatement update, PubkeyChannelObject channelObject) {

        update
                .bind("fragment_index", channelObject.getFragmentIndex())
                .bind("hash", channelObject.getHash())
                .bind("pubkey_a", channelObject.channelKeyA)
                .bind("pubkey_b", channelObject.channelKeyB)
                .bind("signature_a", channelObject.signatureA)
                .bind("signature_b", channelObject.signatureB)
                .bind("txid_anchor", channelObject.txidAnchor)
                .bind("timestamp", channelObject.timestamp);
    }
}
