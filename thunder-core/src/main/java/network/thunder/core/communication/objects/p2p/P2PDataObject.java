package network.thunder.core.communication.objects.p2p;

import network.thunder.core.etc.Tools;

/**
 * Interface for classes that are stored on the node and later are synced with other nodes.
 * <p>
 * We use the getHashAsLong() to determine the fragment block this object should be sent with.
 */
public class P2PDataObject {
    public byte[] getData () {return null;}

    public byte[] getHash () {
        byte[] hash = new byte[20];
        byte[] t = Tools.hashSecret(this.getData());
        System.arraycopy(t, 0, hash, 0, 20);
        return hash;
    }

    public long getHashAsLong () {
        return 0;
    }

    public void verify () {

    }
}
