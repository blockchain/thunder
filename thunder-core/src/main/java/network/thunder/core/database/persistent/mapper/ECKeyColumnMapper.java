package network.thunder.core.database.persistent.mapper;

import org.bitcoinj.core.ECKey;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ECKeyColumnMapper implements ResultColumnMapper<ECKey> {
    public static ECKeyColumnMapper INSTANCE = new ECKeyColumnMapper();

    public static ECKey getKey(byte[] key) {
        try {
            return ECKey.fromPrivate(key);
        } catch (Exception e) {
            return ECKey.fromPublicOnly(key);
        }
    }

    @Override
    public ECKey mapColumn (ResultSet resultSet, int i, StatementContext statementContext) throws SQLException {
        return getKey(resultSet.getBytes(i));
    }

    @Override
    public ECKey mapColumn (ResultSet resultSet, String s, StatementContext statementContext) throws SQLException {
        return getKey(resultSet.getBytes(s));
    }
}
