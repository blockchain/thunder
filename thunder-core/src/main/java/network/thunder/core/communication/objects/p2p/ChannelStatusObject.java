package network.thunder.core.communication.objects.p2p;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ChannelStatusObject {

    public ChannelStatusObject (ResultSet set) throws SQLException {
        this.pubkeyA = set.getBytes("node_a_table.pubkey");
        this.pubkeyB = set.getBytes("node_b_table.pubkey");
        this.infoA = set.getBytes("info_a");
        this.infoB = set.getBytes("info_a");
        this.signatureA = set.getBytes("signature_a");;
        this.signatureB = set.getBytes("signature_b");;
        this.timestamp = set.getInt("timestamp");;
    }

    public byte[] pubkeyA;
    public byte[] pubkeyB;

    public byte[] infoA;
    public byte[] infoB;

    public byte[] signatureA;
    public byte[] signatureB;

    public int timestamp;
}
