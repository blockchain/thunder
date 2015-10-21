package network.thunder.core.communication.objects.p2p;

import network.thunder.core.communication.objects.subobjects.ConnectedPeer;

import java.util.ArrayList;

/**
 * object for housing the information that will be broadcasted through the network.
 */
public class MetaDataBroadcast {

    private byte[] pubkey;
    private int timestamp;
    private byte[] signature;
    private String ip;
    private int port;

    private ArrayList<ConnectedPeer> peerList;

    public MetaDataBroadcast (byte[] pubkey, int timestamp, byte[] signature, String ip, int port, ArrayList<ConnectedPeer> peerList) {
        this.pubkey = pubkey;
        this.timestamp = timestamp;
        this.signature = signature;
        this.ip = ip;
        this.port = port;
        this.peerList = peerList;
    }

    public byte[] getPubkey () {
        return pubkey;
    }

    public int getTimestamp () {
        return timestamp;
    }

    public byte[] getSignature () {
        return signature;
    }

    public String getIp () {
        return ip;
    }

    public int getPort () {
        return port;
    }

    public ArrayList<ConnectedPeer> getPeerList () {
        return peerList;
    }
}
