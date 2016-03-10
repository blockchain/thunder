package network.thunder.core.communication.objects.messages.impl.blockchainlistener.bciapi;

import com.google.gson.JsonObject;

/**
 * This class contains data related to inventory messages that Blockchain.info received
 * for an object.
 */
public class InventoryData {
    private String hash;
    private String type;
    private long initialTime;
    private long lastTime;
    private String initialIP;
    private int nConnected;
    private int relayedCount;
    private int relayedPercent;

    public InventoryData (String hash, String type, long initialTime, long lastTime, String initialIP, int nConnected, int relayedCount, int relayedPercent) {
        this.hash = hash;
        this.type = type;
        this.initialTime = initialTime;
        this.lastTime = lastTime;
        this.initialIP = initialIP;
        this.nConnected = nConnected;
        this.relayedCount = relayedCount;
        this.relayedPercent = relayedPercent;
    }

    public InventoryData (JsonObject i) {
        this(i.get("hash").getAsString(), i.get("type").getAsString(), i.get("initial_time").getAsLong(), i.get("last_time").getAsLong(), i.get("initial_ip").getAsString(), i.get("nconnected").getAsInt(), i.get("relayed_count").getAsInt(), i.get("relayed_percent").getAsInt());
    }

    @Override
    public boolean equals (Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof InventoryData) {
            InventoryData that = (InventoryData) o;
            return (this.hash.equals(that.hash) && this.type.equals(that.type));
        }
        return false;
    }

    /**
     * @return Object hash
     */
    public String getHash () {
        return hash;
    }

    /**
     * @return Object type
     */
    public String getType () {
        return type;
    }

    /**
     * @return The time Blockchain.info first received an inventory message
     * containing a hash for this transaction.
     */
    public long getInitialTime () {
        return initialTime;
    }

    /**
     * @return The last time Blockchain.info received an inventory message
     * containing a hash for this transaction.
     */
    public long getLastTime () {
        return lastTime;
    }

    /**
     * @return IP of the peer from which Blockchain.info first received an inventory message
     * containing a hash for this transaction.
     */
    public String getInitialIP () {
        return initialIP;
    }

    /**
     * @return Number of nodes that Blockchain.info is currently connected to.
     */
    public int getnConnected () {
        return nConnected;
    }

    /**
     * @return Number of nodes Blockchain.info received an inventory message containing
     * a hash for this transaction from.
     */
    public int getRelayedCount () {
        return relayedCount;
    }

    /**
     * @return Ratio of nodes that Blockchain.info received an inventory message
     * containing a hash for this transaction from and the number of connected nodes.
     */
    public int getRelayedPercent () {
        return relayedPercent;
    }
}
