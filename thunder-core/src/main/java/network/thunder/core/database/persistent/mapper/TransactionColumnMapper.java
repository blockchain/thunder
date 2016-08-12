package network.thunder.core.database.persistent.mapper;

import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Transaction;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionColumnMapper implements ResultColumnMapper<Transaction> {
    public static TransactionColumnMapper INSTANCE = new TransactionColumnMapper();

    public static Transaction getTransaction (byte[] tx) {
        return tx == null ? null : new Transaction(Constants.getNetwork(), tx);
    }

    @Override
    public Transaction mapColumn (ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        return getTransaction(r.getBytes(columnNumber));
    }

    @Override
    public Transaction mapColumn (ResultSet r, String columnLabel, StatementContext ctx) throws SQLException {
        return getTransaction(r.getBytes(columnLabel));
    }
}
