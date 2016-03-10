package network.thunder.core.communication.objects.messages.impl.blockchainlistener.bciapi;

import com.google.gson.JsonObject;

/**
 * Represents an unspent transaction output.
 */
public class UnspentOutput {
    private int n;
    private String transactionHash;
    private long transactionIndex;
    private String script;
    private long value;
    private long confirmations;

    public UnspentOutput (int n, String transactionHash, long transactionIndex, String script, long value, long confirmations) {
        this.n = n;
        this.transactionHash = transactionHash;
        this.transactionIndex = transactionIndex;
        this.script = script;
        this.value = value;
        this.confirmations = confirmations;
    }

    public UnspentOutput (JsonObject o) {
        this(o.get("tx_output_n").getAsInt(), o.get("tx_hash").getAsString(), o.get("tx_index").getAsLong(), o.get("script").getAsString(), o.get("value").getAsLong(), o.get("confirmations").getAsLong());
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UnspentOutput that = (UnspentOutput) o;

        if (transactionIndex != that.transactionIndex) {
            return false;
        }
        if (value != that.value) {
            return false;
        }
        if (transactionHash != null ? !transactionHash.equals(that.transactionHash) : that.transactionHash != null) {
            return false;
        }
        return !(script != null ? !script.equals(that.script) : that.script != null);

    }

    @Override
    public int hashCode () {
        int result = transactionHash != null ? transactionHash.hashCode() : 0;
        result = 31 * result + (int) (transactionIndex ^ (transactionIndex >>> 32));
        result = 31 * result + (script != null ? script.hashCode() : 0);
        result = 31 * result + (int) (value ^ (value >>> 32));
        return result;
    }

    /**
     * @return Index of the output in a transaction
     */
    public int getN () {
        return n;
    }

    /**
     * @return Transaction hash
     */
    public String getTransactionHash () {
        return transactionHash;
    }

    /**
     * @return Transaction index
     */
    public long getTransactionIndex () {
        return transactionIndex;
    }

    /**
     * @return Output script
     */
    public String getScript () {
        return script;
    }

    /**
     * @return Value of the output (in satoshi)
     */
    public long getValue () {
        return value;
    }

    /**
     * @return Number of confirmations
     */
    public long getConfirmations () {
        return confirmations;
    }
}
