package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.Channel.Phase;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import org.bitcoinj.core.Sha256Hash;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static network.thunder.core.database.persistent.DBTableNames.CHANNEL;
import static network.thunder.core.database.persistent.DBTableNames.NODE;

public class ChannelRowMapper implements ResultSetMapper<Channel> {
    public static final ChannelRowMapper INSTANCE = new ChannelRowMapper();

    @Override
    public Channel map (int index, ResultSet r, StatementContext ctx) throws SQLException {
        Channel channel = new Channel();

        channel.id = r.getInt(CHANNEL + ".id");
//        channel.hash = Sha256Hash.wrap(r.getBytes(CHANNEL + ".hash"));

        channel.keyClient = ECKeyColumnMapper.INSTANCE.mapColumn(r, CHANNEL + ".key_client", ctx);
        channel.keyServer = ECKeyColumnMapper.INSTANCE.mapColumn(r, CHANNEL + ".key_server", ctx);

        channel.addressClient = AddressColumnMapper.INSTANCE.mapColumn(r, CHANNEL + ".address_client", ctx);
        channel.addressServer = AddressColumnMapper.INSTANCE.mapColumn(r, CHANNEL + ".address_server", ctx);

        channel.masterPrivateKeyClient = r.getBytes(CHANNEL + ".master_priv_key_client");
        channel.masterPrivateKeyServer = r.getBytes(CHANNEL + ".master_priv_key_server");

        channel.shaChainDepthCurrent = r.getInt(CHANNEL + ".sha_chain_depth");

        channel.amountClient = r.getLong(CHANNEL + ".amount_client");
        channel.amountServer = r.getLong(CHANNEL + ".amount_server");

        channel.timestampOpen = r.getInt(CHANNEL + ".timestamp_open");
        channel.timestampForceClose = r.getInt(CHANNEL + ".timestamp_force_close");

        channel.anchorTxHash = Sha256Hash.wrap(r.getBytes(CHANNEL + ".anchor_tx_hash"));

        channel.anchorBlockHeight = r.getInt(CHANNEL + ".anchor_tx_blockheight");
        channel.minConfirmationAnchor = r.getInt(CHANNEL + ".anchor_tx_min_conf");

        channel.spendingTx = TransactionColumnMapper.INSTANCE.mapColumn(r, CHANNEL + "channel_tx_on_chain", ctx);

        channel.channelSignatures = ChannelSignatures.deserialise(r.getString(CHANNEL + ".channel_tx_signatures"));

        channel.csvDelay = r.getInt(CHANNEL + ".csv_delay");
        channel.feePerByte = r.getInt(CHANNEL + ".fee_per_byte");
        channel.phase = Phase.valueOf(r.getString(CHANNEL + ".phase"));

        //JOINED PARAMETERS
        channel.nodeKeyClient = new NodeKey(r.getBytes(NODE + "key"));

        channel.revoHashClientCurrent = RevocationHashRowMapper.map(r, "r_client_current");
        channel.revoHashClientNext = RevocationHashRowMapper.map(r, "r_client_current");

        //CALCULATED FIELDS
        channel.revoHashServerCurrent = new RevocationHash(channel.shaChainDepthCurrent, channel.masterPrivateKeyServer);
        channel.revoHashServerNext = new RevocationHash(channel.shaChainDepthCurrent + 1, channel.masterPrivateKeyServer);

        return channel;
    }

    public static void bindChannelToQuery (Update query, Channel channel) {
        query
                .bind("hash", channel.getHash().getBytes())
                .bind("key_client", channel.keyClient.getPubKey())
                .bind("key_server", channel.keyServer.getPrivKeyBytes())
                .bind("address_client", channel.addressClient.toString())
                .bind("address_server", channel.addressClient.toString())
                .bind("master_priv_key_client", channel.masterPrivateKeyClient)
                .bind("master_priv_key_server", channel.masterPrivateKeyServer)
                .bind("sha_chain_depth", channel.shaChainDepthCurrent)
                .bind("amount_client", channel.amountClient)
                .bind("amount_server", channel.amountServer)
                .bind("timestamp_open", channel.timestampOpen)
                .bind("timestamp_force_close", channel.timestampForceClose)
                .bind("anchor_tx_hash", channel.anchorTxHash.getBytes())
                .bind("anchor_tx_blockheight", channel.anchorBlockHeight)
                .bind("anchor_tx_min_conf", channel.minConfirmationAnchor)
                .bind("channel_tx_on_chain", channel.spendingTx)
                .bind("channel_tx_signatures", channel.channelSignatures.serialize())
                .bind("csv_delay", channel.csvDelay)
                .bind("fee_per_byte", channel.feePerByte)
                .bind("phase", channel.phase.toString());
    }

}
