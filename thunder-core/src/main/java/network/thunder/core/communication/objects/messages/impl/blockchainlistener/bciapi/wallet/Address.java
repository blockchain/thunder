package network.thunder.core.communication.objects.messages.impl.blockchainlistener.bciapi.wallet;

/**
 * Used in combination with the `Wallet` class
 */
public class Address {
    private long balance;
    private String address;
    private String label;
    private long totalReceived;

    public Address (long balance, String address, String label, long totalReceived) {
        this.balance = balance;
        this.address = address;
        this.label = label;
        this.totalReceived = totalReceived;
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

        if (balance != address1.balance) {
            return false;
        }
        if (totalReceived != address1.totalReceived) {
            return false;
        }
        if (address != null ? !address.equals(address1.address) : address1.address != null) {
            return false;
        }
        return !(label != null ? !label.equals(address1.label) : address1.label != null);

    }

    @Override
    public int hashCode () {
        int result = (int) (balance ^ (balance >>> 32));
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (int) (totalReceived ^ (totalReceived >>> 32));
        return result;
    }

    /**
     * @return Balance in satoshi
     */
    public long getBalance () {
        return balance;
    }

    /**
     * @return String representation of the address
     */
    public String getAddress () {
        return address;
    }

    /**
     * @return Label attached to the address
     */
    public String getLabel () {
        return label;
    }

    /**
     * @return Total received amount in satoshi
     */
    public long getTotalReceived () {
        return totalReceived;
    }
}
