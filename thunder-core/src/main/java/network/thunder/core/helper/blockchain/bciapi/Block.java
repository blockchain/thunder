package network.thunder.core.helper.blockchain.bciapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a full representation of a block. For simpler representations, see
 * `SimpleBlock` and `LatestBlock`.
 */
public class Block extends SimpleBlock {
    private int version;
    private String previousBlockHash;
    private String merkleRoot;
    private long bits;
    private long fees;
    private long nonce;
    private long size;
    private long index;
    private long receivedTime;
    private String relayedBy;
    private List<Transaction> transactions;

    public Block (long height, String hash, long time, boolean mainChain, int version, String previousBlockHash, String merkleRoot, long bits, long fees,
                  long nonce, long size, long index, long receivedTime, String relayedBy, List<Transaction> transactions) {
        super(height, hash, time, mainChain);
        this.version = version;
        this.previousBlockHash = previousBlockHash;
        this.merkleRoot = merkleRoot;
        this.bits = bits;
        this.fees = fees;
        this.nonce = nonce;
        this.size = size;
        this.index = index;
        this.receivedTime = receivedTime;
        this.relayedBy = relayedBy;
        this.transactions = transactions;
    }

    public Block (JsonObject b) {
        this(b.get("height").getAsLong(), b.get("hash").getAsString(), b.get("time").getAsLong(), b.get("main_chain").getAsBoolean(), b.get("ver").getAsInt()
                , b.get("prev_block").getAsString(), b.get("mrkl_root").getAsString(), b.get("bits").getAsLong(), b.get("fee").getAsLong(), b.get("nonce")
                        .getAsLong(), b.get("size").getAsLong(), b.get("block_index").getAsLong(), b.has("received_time") ? b.get("received_time").getAsLong
                        () : b.get("time").getAsLong(), b.has("relayed_by") ? b.get("relayed_by").getAsString() : null, null);

        transactions = new ArrayList<Transaction>();
        for (JsonElement txElem : b.get("tx").getAsJsonArray()) {
            transactions.add(new Transaction(txElem.getAsJsonObject(), super.getHeight(), false));
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
        if (!super.equals(o)) {
            return false;
        }

        Block block = (Block) o;

        if (version != block.version) {
            return false;
        }
        if (fees != block.fees) {
            return false;
        }
        if (nonce != block.nonce) {
            return false;
        }
        if (size != block.size) {
            return false;
        }
        if (index != block.index) {
            return false;
        }
        if (!previousBlockHash.equals(block.previousBlockHash)) {
            return false;
        }
        return merkleRoot.equals(block.merkleRoot);

    }

    @Override
    public int hashCode () {
        int result = version;
        result = 31 * result + previousBlockHash.hashCode();
        result = 31 * result + merkleRoot.hashCode();
        result = 31 * result + (int) (fees ^ (fees >>> 32));
        result = 31 * result + (int) (nonce ^ (nonce >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (index ^ (index >>> 32));
        return result;
    }

    /**
     * @return Block version as specified by the protocol
     */
    public int getVersion () {
        return version;
    }

    /**
     * @return Hash of the previous block
     */
    public String getPreviousBlockHash () {
        return previousBlockHash;
    }

    /**
     * @return Merkle root of the block
     */
    public String getMerkleRoot () {
        return merkleRoot;
    }

    /**
     * @return Representation of the difficulty target for this block
     */
    public long getBits () {
        return bits;
    }

    /**
     * @return Total transaction fees from this block (in satoshi)
     */
    public long getFees () {
        return fees;
    }

    /**
     * @return Block nonce
     */
    public long getNonce () {
        return nonce;
    }

    /**
     * @return Serialized size of this block
     */
    public long getSize () {
        return size;
    }

    /**
     * @return Index of this block
     */
    public long getIndex () {
        return index;
    }

    /**
     * @return The time this block was received by Blockchain.info
     */
    public long getReceivedTime () {
        return receivedTime;
    }

    /**
     * @return hostname address that relayed the block
     */
    public String getRelayedBy () {
        return relayedBy;
    }

    /**
     * @return Transactions in the block
     */
    public List<Transaction> getTransactions () {
        return transactions;
    }
}
