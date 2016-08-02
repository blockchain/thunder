package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.layer.high.RevocationHash;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static network.thunder.core.database.persistent.DBTableNames.REVO_HASH;

public class RevocationHashRowMapper implements ResultSetMapper<RevocationHash> {
    public static final RevocationHashRowMapper INSTANCE = new RevocationHashRowMapper();

    public static RevocationHash map (ResultSet r, String prefix) throws SQLException {
        byte[] hash = r.getBytes(prefix + "hash");
        byte[] secret = r.getBytes(prefix + "secret");
        int depth = r.getInt(prefix + "depth");

        return new RevocationHash(depth, secret, hash);
    }

    public static void bindChannelToQuery (Update update, RevocationHash hash) {
        update
                .bind("depth", hash.index)
                .bind("hash", hash.secretHash)
                .bind("secret", hash.secret);
    }

    @Override
    public RevocationHash map (int index, ResultSet r, StatementContext ctx) throws SQLException {
        return map(r, REVO_HASH + ".");
    }
}
