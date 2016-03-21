package network.thunder.core.helper.blockchain.bciapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an address.
 */
public class Address {
    private String hash160;
    private String address;
    private long totalReceived;
    private long totalSent;
    private long finalBalance;
    private List<Transaction> transactions;

    public Address (String hash160, String address, long totalReceived, long totalSent, long finalBalance, List<Transaction> transactions) {
        this.hash160 = hash160;
        this.address = address;
        this.totalReceived = totalReceived;
        this.totalSent = totalSent;
        this.finalBalance = finalBalance;
        this.transactions = transactions;
    }

    public Address (JsonObject a) {

        this(a.has("hash160") ? a.get("hash160").getAsString() : "", a.has("address") ? a.get("address").getAsString() : "", a.has("total_received") ? a.get("total_received").getAsLong() : 0, a.has("total_sent") ? a.get("total_sent").getAsLong() : 0, a.has("final_balance") ? a.get("final_balance").getAsLong() : 0, null);

        transactions = new ArrayList<Transaction>();
        for (JsonElement txElem : a.get("txs").getAsJsonArray()) {
            JsonObject addrObj = txElem.getAsJsonObject();
            transactions.add(new Transaction(addrObj));
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

        Address address1 = (Address) o;

        if (totalReceived != address1.totalReceived) {
            return false;
        }
        if (totalSent != address1.totalSent) {
            return false;
        }
        if (finalBalance != address1.finalBalance) {
            return false;
        }
        if (!hash160.equals(address1.hash160)) {
            return false;
        }
        return address.equals(address1.address);

    }

    @Override
    public int hashCode () {
        int result = hash160.hashCode();
        result = 31 * result + address.hashCode();
        result = 31 * result + (int) (totalReceived ^ (totalReceived >>> 32));
        result = 31 * result + (int) (totalSent ^ (totalSent >>> 32));
        result = 31 * result + (int) (finalBalance ^ (finalBalance >>> 32));
        return result;
    }

    /**
     * @return Hash160 representation of the address
     */
    public String getHash160 () {
        return hash160;
    }

    /**
     * @return Base58Check representation of the address
     */
    public String getAddress () {
        return address;
    }

    /**
     * @return Total amount received (in satoshi)
     */
    public long getTotalReceived () {
        return totalReceived;
    }

    /**
     * @return Total amount sent (in satoshi)
     */
    public long getTotalSent () {
        return totalSent;
    }

    /**
     * @return Final balance of the address (in satoshi)
     */
    public long getFinalBalance () {
        return finalBalance;
    }

    /**
     * @return List of transactions associated with this address
     */
    public List<Transaction> getTransactions () {
        return transactions;
    }

}
