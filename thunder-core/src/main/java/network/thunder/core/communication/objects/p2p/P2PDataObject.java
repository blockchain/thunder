package network.thunder.core.communication.objects.p2p;

import network.thunder.core.communication.Message;
import network.thunder.core.etc.Tools;

/**
 * Interface for classes that are stored on the node and later are synced with other nodes.
 * <p>
 * We use the getHashAsLong() to determine the fragment block this object should be sent with.
 */
public abstract class P2PDataObject implements Message {
    public abstract byte[] getData ();

    public byte[] getHash () {
        byte[] hash = new byte[20];
        byte[] t = Tools.hashSecret(this.getData());
        System.arraycopy(t, 0, hash, 0, 20);
        return hash;
    }

    public abstract long getHashAsLong ();

    public abstract void verify ();
}
