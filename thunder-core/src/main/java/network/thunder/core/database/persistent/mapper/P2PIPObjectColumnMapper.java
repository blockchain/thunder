package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static network.thunder.core.database.persistent.DBTableNames.NODE;
import static network.thunder.core.database.persistent.DBTableNames.P2P_IP;

public class P2PIPObjectColumnMapper implements ResultSetMapper<PubkeyIPObject> {
    public static final P2PIPObjectColumnMapper INSTANCE = new P2PIPObjectColumnMapper();

    @Override
    public PubkeyIPObject map (int index, ResultSet set, StatementContext ctx) throws SQLException {
        PubkeyIPObject ipObject = new PubkeyIPObject();

        ipObject.hostname = set.getString(P2P_IP+".host");
        ipObject.port = set.getInt(P2P_IP+".port");
        ipObject.timestamp = set.getInt(P2P_IP+".timestamp");
        ipObject.signature = set.getBytes(P2P_IP+".signature");
        ipObject.pubkey = set.getBytes(NODE+".pubkey");

        return ipObject;
    }

    public static void bindChannelToQuery (SQLStatement update, PubkeyIPObject ipObject) {
        update
                .bind("fragment_index", ipObject.getFragmentIndex())
                .bind("hash", ipObject.getHash())
                .bind("timestamp", ipObject.timestamp)
                .bind("host", ipObject.hostname)
                .bind("port", ipObject.port)
                .bind("signature", ipObject.signature);
    }
}
