package network.thunder.core.helper.blockchain.bciapi.wallet;

/**
 * Used in response to the `send` and `sendMany` methods in the `Wallet` class.
 */
public class PaymentResponse {
    private String message;
    private String txHash;
    private String notice;

    public PaymentResponse (String message, String txHash, String notice) {
        this.message = message;
        this.txHash = txHash;
        this.notice = notice;
    }

    @Override
    public boolean equals (Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof PaymentResponse) {
            PaymentResponse that = (PaymentResponse) o;
            return (this.getMessage().equals(that.getMessage()) && this.getTxHash().equals(that.getTxHash()) && this.getNotice().equals(that.getNotice()));
        }
        return false;
    }

    /**
     * @return Response message from the server
     */
    public String getMessage () {
        return message;
    }

    /**
     * @return Transaction hash
     */
    public String getTxHash () {
        return txHash;
    }

    /**
     * @return Additional response message from the server
     */
    public String getNotice () {
        return notice;
    }

}
