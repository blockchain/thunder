package network.thunder.core.helper.blockchain.bciapi.blockexplorer;

import com.google.gson.JsonObject;

/**
 * Represents a transaction output.
 */
public class Output {
    private int n;
    private long value;
    private String address;
    private long txIndex;
    private String script;
    private boolean spent;
    private boolean spentToAddress;

    public Output (int n, long value, String address, long txIndex, String script, boolean spent) {
        this.n = n;
        this.value = value;
        this.address = address;
        this.txIndex = txIndex;
        this.script = script;
        this.spent = spent;
        if (address != "") {
            spentToAddress = true;
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

        Output output = (Output) o;

        if (value != output.value) {
            return false;
        }
        if (txIndex != output.txIndex) {
            return false;
        }
        return !(script != null ? !script.equals(output.script) : output.script != null);

    }

    @Override
    public int hashCode () {
        int result = (int) (value ^ (value >>> 32));
        result = 31 * result + (int) (txIndex ^ (txIndex >>> 32));
        result = 31 * result + (script != null ? script.hashCode() : 0);
        return result;
    }

    public Output (JsonObject o) {
        this(o, o.get("spent").getAsBoolean());
    }

    public Output (JsonObject o, boolean spent) {
        this(o.get("n").getAsInt(), o.get("value").getAsLong(), o.has("addr") ? o.get("addr").getAsString() : "", o.get("tx_index").getAsLong(), o.get("script").getAsString(), spent);
    }

    /**
     * @return Index of the output in a transaction
     */
    public int getN () {
        return n;
    }

    /**
     * @return Value of the output (in satoshi)
     */
    public long getValue () {
        return value;
    }

    /**
     * @return Address that the output belongs to
     */
    public String getAddress () {
        return address;
    }

    /**
     * @return Transaction index
     */
    public long getTxIndex () {
        return txIndex;
    }

    /**
     * @return Output script
     */
    public String getScript () {
        return script;
    }

    /**
     * @return Whether the output is spent
     */
    public boolean isSpent () {
        return spent;
    }

    /**
     * @return Whether the output pays to an address.
     */
    public boolean isSpentToAddress () {
        return spentToAddress;
    }
}
