package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.NodeKey;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static network.thunder.core.database.persistent.DBTableNames.NODE;

public class NodeKeyRowMapper implements ResultSetMapper<NodeKey> {
    public static final NodeKeyRowMapper INSTANCE = new NodeKeyRowMapper();

    @Override
    public NodeKey map (int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new NodeKey(r.getBytes(NODE + ".pubkey"));
    }
}
