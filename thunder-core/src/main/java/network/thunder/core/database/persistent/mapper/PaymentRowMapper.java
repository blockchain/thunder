package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.database.objects.PaymentStatus;
import network.thunder.core.etc.Tools;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static network.thunder.core.database.persistent.DBTableNames.*;

public class PaymentRowMapper implements ResultSetMapper<PaymentData> {
    public static final PaymentRowMapper INSTANCE = new PaymentRowMapper();

    @Override
    public PaymentData map (int index, ResultSet r, StatementContext ctx) throws SQLException {
        PaymentData payment = new PaymentData();
        payment.paymentId = r.getInt(PAYMENT+".id");
        payment.sending = Tools.intToBool(r.getInt(PAYMENT + ".sending"));
        payment.amount = r.getLong(PAYMENT + ".amount");

        payment.timestampOpen = r.getInt(PAYMENT + ".timestamp_added");
        payment.timestampRefund = r.getInt(PAYMENT + ".timestamp_refund");
        payment.timestampSettled = r.getInt(PAYMENT + ".timestamp_settled");

        payment.onionObject = new OnionObject(r.getBytes(ONION + ".data"));

        payment.status = PaymentStatus.valueOf(r.getString(PAYMENT + ".phase"));

        payment.secret = new PaymentSecret(r.getBytes(SECRET + ".secret"), r.getBytes(SECRET + ".hash"));

        return payment;
    }

    public static Update bindChannelToQuery (Update query, PaymentData payment) {
        return query
                .bind("secret_hash", payment.secret.secret)
                .bind("sending", Tools.boolToInt(payment.sending))
                .bind("amount", payment.amount)
                .bind("phase", payment.status.toString())
                .bind("secret_hash", payment.secret.hash)
                .bind("onion_hash", Tools.hashSecret(payment.onionObject.data))
                .bind("timestamp_added", payment.timestampOpen)
                .bind("timestamp_refund", payment.timestampRefund)
                .bind("timestamp_settled", payment.timestampSettled)
                .bind("version_added", 0)
                .bind("version_settled", 0);
    }

}
