package network.thunder.core.helper.blockchain.bciapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a transaction.
 */
public class Transaction {
    private boolean doubleSpend;
    private long blockHeight;
    private long time;
    private String relayedBy;
    private String hash;
    private long index;
    private int version;
    private long size;
    private List<Input> inputs;
    private List<Output> outputs;

    public Transaction (boolean doubleSpend, long blockHeight, long time, String relayedBy, String hash, long index, int version, long size, List<Input> inputs, List<Output> outputs) {
        this.doubleSpend = doubleSpend;
        this.blockHeight = blockHeight;
        this.time = time;
        this.relayedBy = relayedBy;
        this.hash = hash;
        this.index = index;
        this.version = version;
        this.size = size;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public Transaction (JsonObject t) {
        this(t, t.has("block_height") ? t.get("block_height").getAsLong() : -1, t.has("double_spend") ? t.get("double_spend").getAsBoolean() : false);
    }

    public Transaction (JsonObject t, long blockHeight, boolean doubleSpend) {
        this(doubleSpend, blockHeight, t.get("time").getAsLong(), t.get("relayed_by").getAsString(), t.get("hash").getAsString(), t.get("tx_index").getAsLong(), t.get("ver").getAsInt(), t.get("size").getAsLong(), null, null);

        inputs = new ArrayList<Input>();
        for (JsonElement inputElem : t.get("inputs").getAsJsonArray()) {
            inputs.add(new Input(inputElem.getAsJsonObject()));
        }

        outputs = new ArrayList<Output>();
        for (JsonElement outputElem : t.get("out").getAsJsonArray()) {
            outputs.add(new Output(outputElem.getAsJsonObject()));
        }
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Transaction that = (Transaction) o;

        if (index != that.index) {
            return false;
        }
        if (version != that.version) {
            return false;
        }
        if (size != that.size) {
            return false;
        }
        if (hash != null ? !hash.equals(that.hash) : that.hash != null) {
            return false;
        }
        if (inputs != null ? !inputs.equals(that.inputs) : that.inputs != null) {
            return false;
        }
        return !(outputs != null ? !outputs.equals(that.outputs) : that.outputs != null);

    }

    @Override
    public int hashCode () {
        int result = hash != null ? hash.hashCode() : 0;
        result = 31 * result + (int) (index ^ (index >>> 32));
        result = 31 * result + version;
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (inputs != null ? inputs.hashCode() : 0);
        result = 31 * result + (outputs != null ? outputs.hashCode() : 0);
        return result;
    }

    /**
     * @return Whether the transaction is a double spend
     */
    public boolean isDoubleSpend () {
        return doubleSpend;
    }

    /**
     * @return Block height of the parent block. -1 for unconfirmed transactions.
     */
    public long getBlockHeight () {
        return blockHeight;
    }

    /**
     * @return Timestamp of the transaction
     */
    public long getTime () {
        return time;
    }

    /**
     * @return hostname address that relayed the transaction
     */
    public String getRelayedBy () {
        return relayedBy;
    }

    /**
     * @return Transaction hash
     */
    public String getHash () {
        return hash;
    }

    /**
     * @return Transaction index
     */
    public long getIndex () {
        return index;
    }

    /**
     * @return Transaction format version
     */
    public int getVersion () {
        return version;
    }

    /**
     * @return Serialized size of the transaction
     */
    public long getSize () {
        return size;
    }

    /**
     * @return List of inputs
     */
    public List<Input> getInputs () {
        return inputs;
    }

    /**
     * @return List of outputs
     */
    public List<Output> getOutputs () {
        return outputs;
    }
}
