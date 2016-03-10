package network.thunder.core.communication.objects.messages.impl.blockchainlistener.bciapi.blockexplorer;

import com.google.gson.JsonObject;

/**
 * Simple representation of a block
 */
public class SimpleBlock {
    private long height;
    private String hash;
    private long time;
    private boolean mainChain;

    public SimpleBlock (long height, String hash, long time, boolean mainChain) {
        this.height = height;
        this.hash = hash;
        this.time = time;
        this.mainChain = mainChain;
    }

    public SimpleBlock (JsonObject b) {
        this(b.get("height").getAsLong(), b.get("hash").getAsString(), b.get("time").getAsLong(), b.get("main_chain").getAsBoolean());
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleBlock that = (SimpleBlock) o;

        if (height != that.height) {
            return false;
        }
        if (time != that.time) {
            return false;
        }
        if (mainChain != that.mainChain) {
            return false;
        }
        return !(hash != null ? !hash.equals(that.hash) : that.hash != null);

    }

    @Override
    public int hashCode () {
        int result = (int) (height ^ (height >>> 32));
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + (mainChain ? 1 : 0);
        return result;
    }

    /**
     * @return Block height
     */
    public long getHeight () {
        return height;
    }

    /**
     * @return Block hash
     */
    public String getHash () {
        return hash;
    }

    /**
     * @return Block timestamp set by the miner (unix time in seconds)
     */
    public long getTime () {
        return time;
    }

    /**
     * @return Whether the block is on the main chain
     */
    public boolean isMainChain () {
        return mainChain;
    }
}
