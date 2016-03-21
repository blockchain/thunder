package network.thunder.core.helper.blockchain.bciapi.createwallet;

/**
 * Used in response to the `create` method in the `CreateWallet` class.
 */
public class CreateWalletResponse {
    private String identifier;
    private String address;
    private String link;

    public CreateWalletResponse (String identifier, String address, String link) {
        this.identifier = identifier;
        this.address = address;
        this.link = link;
    }

    /**
     * @return Wallet identifier (GUID)
     */
    public String getIdentifier () {
        return identifier;
    }

    /**
     * @return First address in the wallet
     */
    public String getAddress () {
        return address;
    }

    /**
     * @return Link to the wallet
     */
    public String getLink () {
        return link;
    }
}
