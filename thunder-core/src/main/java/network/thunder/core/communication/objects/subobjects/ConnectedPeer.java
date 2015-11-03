package network.thunder.core.communication.objects.subobjects;

/**
 * Information about a connected peer. We pass these through the network for them to store in a reputation system...
 * <p>
 * Todo: We stick with basic types for now, as we don't know if this will be the final layout..
 * Todo: Figure out if we want to keep the fee/amount in here, as we would want to have the reputation system with just the pubkey+reputation+signature, but
 * adding another signature doesn't really seem reasonable either...
 */
public class ConnectedPeer {

    private byte[] pubkey;
    private float fee;
    private float amout;
    private short reputation;
    private byte[] signature;

    public ConnectedPeer (byte[] pubkey, float fee, float amout, short reputation, byte[] signature) {
        this.pubkey = pubkey;
        this.fee = fee;
        this.amout = amout;
        this.reputation = reputation;
        this.signature = signature;
    }

    public float getAmout () {
        return amout;
    }

    public float getFee () {
        return fee;
    }

    public byte[] getPubkey () {
        return pubkey;
    }

    public short getReputation () {
        return reputation;
    }

    public byte[] getSignature () {
        return signature;
    }
}
