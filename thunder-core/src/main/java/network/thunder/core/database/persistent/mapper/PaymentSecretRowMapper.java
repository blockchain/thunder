package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static network.thunder.core.database.persistent.DBTableNames.SECRET;

public class PaymentSecretRowMapper implements ResultSetMapper<PaymentSecret> {
    public static final PaymentSecretRowMapper INSTANCE = new PaymentSecretRowMapper();

    public static PaymentSecret map (ResultSet r, String prefix) throws SQLException {
        byte[] hash = r.getBytes(prefix + "hash");
        byte[] secret = r.getBytes(prefix + "secret");

        return new PaymentSecret(secret, hash);
    }

    public static void bindChannelToQuery (Update update, PaymentSecret hash) {
        update
                .bind("hash", hash.hash)
                .bind("secret", hash.secret);
    }

    @Override
    public PaymentSecret map (int index, ResultSet r, StatementContext ctx) throws SQLException {
        return map(r, SECRET + ".");
    }
}
