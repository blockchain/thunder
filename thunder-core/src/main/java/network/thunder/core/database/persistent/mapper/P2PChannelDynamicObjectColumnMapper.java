package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.database.persistent.DBTableNames;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class P2PChannelDynamicObjectColumnMapper implements ResultSetMapper<ChannelStatusObject> {
    public static P2PChannelDynamicObjectColumnMapper INSTANCE = new P2PChannelDynamicObjectColumnMapper();

    @Override
    public ChannelStatusObject map (int index, ResultSet set, StatementContext ctx) throws SQLException {
        ChannelStatusObject channelObject = new ChannelStatusObject();

        //H2 does not support table aliases
        channelObject.pubkeyA = set.getBytes("node_b_pubkey");
        channelObject.pubkeyB = set.getBytes("node_a_pubkey");
        channelObject.infoA = set.getBytes(DBTableNames.P2P_CHANNEL_DYNAMIC + ".info_a");
        channelObject.infoB = set.getBytes(DBTableNames.P2P_CHANNEL_DYNAMIC + ".info_b");
        channelObject.signatureA = set.getBytes(DBTableNames.P2P_CHANNEL_DYNAMIC + ".signature_a");
        channelObject.signatureB = set.getBytes(DBTableNames.P2P_CHANNEL_DYNAMIC + ".signature_b");
        channelObject.timestamp = set.getInt(DBTableNames.P2P_CHANNEL_DYNAMIC + ".timestamp");

        return channelObject;
    }

    public static void bindChannelToQuery (SQLStatement update, ChannelStatusObject channelObject) {

        update
                .bind("fragment_index", channelObject.getFragmentIndex())
                .bind("hash", channelObject.getHash())
                .bind("timestamp", channelObject.timestamp)
                .bind("signature_a", channelObject.signatureA)
                .bind("signature_b", channelObject.signatureB)
                .bind("info_a", channelObject.infoA)
                .bind("info_b", channelObject.infoB);
    }

}
