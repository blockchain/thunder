package network.thunder.core.communication.objects.p2p;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class PubkeyChannelObject implements P2PDataObject {
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
