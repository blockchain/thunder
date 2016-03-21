package network.thunder.core.communication.layer.high.payments.messages;

/**
 * Created by matsjerratsch on 08/12/2015.
 */
public class OnionObject implements LNPayment {
    public final static int MAX_HOPS = 10;

    public final static int KEY_LENGTH = 33;
    public final static int HMAC_LENGTH = 20;
    public final static int DATA_LENGTH = 50;
    public final static int TOTAL_LENGTH = KEY_LENGTH + HMAC_LENGTH + DATA_LENGTH;

    public byte[] data;

    public OnionObject (byte[] data) {
        this.data = data;
    }

    @Override
    public void verify () {

    }
}
