package network.thunder.core.helper.blockchain.bciapi.receive;

/**
 * This class is used as a response object to the `Receive.receive` method.
 */
public class ReceiveResponse {
    private int feePercent;
    private String destinationAddress;
    private String inputAddress;
    private String callbackUrl;

    public ReceiveResponse (int feePercent, String destinationAddress, String inputAddress, String callbackUrl) {
        this.feePercent = feePercent;
        this.destinationAddress = destinationAddress;
        this.inputAddress = inputAddress;
        this.callbackUrl = callbackUrl;
    }

    /**
     * @return Forwarding fee
     */
    public int getFeePercent () {
        return feePercent;
    }

    /**
     * @return Destination address where the funds will be forwarded
     */
    public String getDestinationAddress () {
        return destinationAddress;
    }

    /**
     * @return Input address where the funds should be sent
     */
    public String getInputAddress () {
        return inputAddress;
    }

    /**
     * @return Callback URI that will be called upon payment
     */
    public String getCallbackUrl () {
        return callbackUrl;
    }
}