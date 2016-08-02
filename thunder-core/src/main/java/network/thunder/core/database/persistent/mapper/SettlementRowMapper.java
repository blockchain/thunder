package network.thunder.core.database.persistent.mapper;

import network.thunder.core.database.objects.ChannelSettlement;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Sha256Hash;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static network.thunder.core.database.persistent.DBTableNames.SETTLEMENT;

public class SettlementRowMapper implements ResultSetMapper<ChannelSettlement> {
    @Override
    public ChannelSettlement map (int index, ResultSet r, StatementContext ctx) throws SQLException {
        ChannelSettlement channelSettlement = new ChannelSettlement();

        channelSettlement.settlementId = r.getInt(SETTLEMENT + ".id");

        int phase = r.getInt(SETTLEMENT + ".phase");
        if (phase == 0) {
            channelSettlement.phase = ChannelSettlement.SettlementPhase.UNSETTLED;
        } else {
            channelSettlement.phase = ChannelSettlement.SettlementPhase.SETTLED;
        }

        channelSettlement.paymentData = PaymentRowMapper.INSTANCE.map(index, r, ctx);

        channelSettlement.channelTx = ColumnMapper.getTransaction(r.getBytes(SETTLEMENT + ".channel_tx"));
        channelSettlement.secondTx = ColumnMapper.getTransaction(r.getBytes(SETTLEMENT + ".second_tx"));
        channelSettlement.thirdTx = ColumnMapper.getTransaction(r.getBytes(SETTLEMENT + ".third_tx"));

        channelSettlement.channelTxHeight = r.getInt(SETTLEMENT + ".channel_tx_height");
        channelSettlement.secondTxHeight = r.getInt(SETTLEMENT + ".second_tx_height");
        channelSettlement.thirdTxHeight = r.getInt(SETTLEMENT + ".third_tx_height");

        if (channelSettlement.channelTx != null) {
            channelSettlement.channelOutput = channelSettlement.channelTx.getOutput(r.getLong(SETTLEMENT + ".channel_tx_output"));
        }
        if (channelSettlement.secondTx != null) {
            channelSettlement.secondOutput = channelSettlement.secondTx.getOutput(r.getLong(SETTLEMENT + ".second_tx_output"));
        }
        if (channelSettlement.thirdTx != null) {
            channelSettlement.thirdOutput = channelSettlement.thirdTx.getOutput(r.getLong(SETTLEMENT + ".third_tx_output"));
        }

        channelSettlement.channelHash = Sha256Hash.wrap(r.getBytes(SETTLEMENT + ".channel_hash"));

        channelSettlement.timeToSettle = r.getInt(SETTLEMENT + ".timestamp_to_settle");

        channelSettlement.cheated = Tools.intToBool(r.getInt(SETTLEMENT + ".cheated"));
        channelSettlement.payment = Tools.intToBool(r.getInt(SETTLEMENT + ".is_payment"));
        channelSettlement.ourChannelTx = Tools.intToBool(r.getInt(SETTLEMENT + ".our_channel_tx"));

        return channelSettlement;
    }

    public Query<Map<String, Object>> bindChannelToQuery (Query<Map<String, Object>> query, ChannelSettlement settlement) {
        return query
                .bind(SETTLEMENT+".id", settlement.settlementId)
                .bind(SETTLEMENT+".channel_hash", settlement.channelHash)
                .bind(SETTLEMENT+".phase", Tools.boolToInt(settlement.phase == ChannelSettlement.SettlementPhase.SETTLED))
                .bind(SETTLEMENT+".timestamp_to_settle", settlement.timeToSettle)
                .bind(SETTLEMENT+".our_channel_tx", Tools.boolToInt(settlement.ourChannelTx))
                .bind(SETTLEMENT+".cheated", Tools.boolToInt(settlement.cheated))
                .bind(SETTLEMENT+".is_payment", Tools.boolToInt(settlement.payment))
                .bind(SETTLEMENT+".revocation_hash", settlement.revocationHash.secretHash)
                .bind(SETTLEMENT+".payment_id", settlement.paymentData.paymentId)
                .bind(SETTLEMENT+".channel_tx", settlement.channelTx.bitcoinSerialize())
                .bind(SETTLEMENT+".second_tx", settlement.secondTx.bitcoinSerialize())
                .bind(SETTLEMENT+".third_tx", settlement.thirdTx.bitcoinSerialize())
                .bind(SETTLEMENT+".channel_tx_height", settlement.channelTxHeight)
                .bind(SETTLEMENT+".second_tx_height", settlement.secondTxHeight)
                .bind(SETTLEMENT+".third_tx_height", settlement.thirdTxHeight)
                .bind(SETTLEMENT+".channel_tx_output", settlement.channelOutput.getIndex())
                .bind(SETTLEMENT+".second_tx_output", settlement.secondOutput.getIndex())
                .bind(SETTLEMENT+".third_tx_output", settlement.thirdOutput.getIndex());
    }

}
