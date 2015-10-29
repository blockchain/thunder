package network.thunder.server.api;

import network.thunder.server.database.objects.Payment;
import network.thunder.server.etc.Tools;

import java.nio.channels.Channel;
import java.security.NoSuchAlgorithmException;

public class PaymentReceiveRequest {
    private static final String PREFIX = "4";

    String id;
    String typeOfId;

    String secretHash;
    Payment payment;

    long amount;

    public PaymentReceiveRequest (Channel channel, Payment p) {
        //		super(channel, p);
        payment = p;
        secretHash = p.getSecretHash();

        amount = p.getAmount();

        //		id = p.getPubKeyReceiver();
    }

    public String getAddress () throws NoSuchAlgorithmException {
        String a = PREFIX + typeOfId + id + secretHash;
        String hash = Tools.getFourCharacterHash(a);

        return a + hash;
    }

}
