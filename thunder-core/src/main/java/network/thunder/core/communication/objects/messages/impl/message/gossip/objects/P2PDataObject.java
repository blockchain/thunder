package network.thunder.core.communication.objects.messages.impl.message.gossip.objects;

import network.thunder.core.communication.Message;
import network.thunder.core.etc.Tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for classes that are stored on the node and later are synced with other nodes.
 * <p>
 * We use the getHashAsLong() to determine the fragment block this object should be sent with.
 */
public abstract class P2PDataObject implements Message {
    public final static int NUMBER_OF_FRAGMENTS = 2;
    public final static long FRAGMENT_SIZE = Long.MAX_VALUE / NUMBER_OF_FRAGMENTS;

    public abstract byte[] getData ();

    public byte[] getHash () {
        byte[] hash = new byte[20];
        byte[] t = Tools.hashSecret(this.getData());
        System.arraycopy(t, 0, hash, 0, 20);
        return hash;
    }

    public int getFragmentIndex () {
        return (int) (getHashAsLong() / FRAGMENT_SIZE + 1);
    }

    public abstract long getHashAsLong ();

    public abstract void verify ();

    public static List<P2PDataObject> generaliseList (List<PubkeyIPObject> list) {
        List<P2PDataObject> generalList = new ArrayList<>(list);
        return generalList;
    }
}
