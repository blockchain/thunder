package network.thunder.core.communication.objects.p2p;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class PubkeyChannelObject implements P2PDataObject {

    public PubkeyChannelObject () {
    }

    public PubkeyChannelObject (ResultSet set) throws SQLException {
        this.secretAHash = set.getBytes("secret_a_hash");
        this.secretBHash = set.getBytes("secret_b_hash");
        this.pubkeyB1 = set.getBytes("pubkey_b");
        this.pubkeyB2 = set.getBytes("pubkey_b_dash");
        this.pubkeyA1 = set.getBytes("pubkey_a");
        this.pubkeyA2 = set.getBytes("pubkey_a_dash");
        this.txidAnchor = set.getBytes("txid_anchor");
        this.signatureA = set.getBytes("signature_a");
        this.signatureB = set.getBytes("signature_b");

        this.pubkeyA = set.getBytes("node_a_table.pubkey");
        this.pubkeyB = set.getBytes("node_b_table.pubkey");
    }

    public byte[] secretAHash;
    public byte[] secretBHash;
    public byte[] pubkeyB;
    public byte[] pubkeyB1;
    public byte[] pubkeyB2;
    public byte[] pubkeyA;
    public byte[] pubkeyA1;
    public byte[] pubkeyA2;
    public byte[] txidAnchor;
    public byte[] signatureA;
    public byte[] signatureB;

    @Override
    public long getHash () {
        return 0;
    }
}
