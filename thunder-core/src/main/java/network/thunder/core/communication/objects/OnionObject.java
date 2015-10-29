package network.thunder.core.communication.objects;

/**
 * Created by matsjerratsch on 07/10/2015.
 */
public class OnionObject {

    public byte[] pubkeyOnion;

    public byte[] hmac;

    public byte[] message;

    public byte[] iv;
    public byte[] payload;

    public byte[] padding;

}
