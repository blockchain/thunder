package network.thunder.core.communication.objects.messages.impl.blockchainlistener.bciapi.blockexplorer;

import com.google.gson.JsonObject;

/**
 * Represents a transaction input. If the `previousOutput` object is null, this is a
 * coinbase input.
 */
public class Input {
    private Output previousOutput;
    private long sequence;
    private String scriptSignature;

    public Input (Output previousOutput, long sequence, String scriptSignature) {
        this.previousOutput = previousOutput;
        this.sequence = sequence;
        this.scriptSignature = scriptSignature;
    }

    public Input (JsonObject i) {
        if (i.has("prev_out")) {
            this.previousOutput = new Output(i.get("prev_out").getAsJsonObject(), true);
        }

        this.sequence = i.get("sequence").getAsLong();
        this.scriptSignature = i.get("script").getAsString();
    }

    /**
     * @return Previous output. If null, this is a coinbase input.
     */
    public Output getPreviousOutput () {
        return previousOutput;
    }

    /**
     * @return Sequence number of the input
     */
    public long getSequence () {
        return sequence;
    }

    /**
     * @return Script signature
     */
    public String getScriptSignature () {
        return scriptSignature;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Input input = (Input) o;

        if (sequence != input.sequence) {
            return false;
        }
        if (!previousOutput.equals(input.previousOutput)) {
            return false;
        }
        return scriptSignature.equals(input.scriptSignature);

    }

    @Override
    public int hashCode () {
        int result = previousOutput.hashCode();
        result = 31 * result + (int) (sequence ^ (sequence >>> 32));
        result = 31 * result + scriptSignature.hashCode();
        return result;
    }
}
