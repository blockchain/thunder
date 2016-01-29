package network.thunder.core.communication.objects.messages.impl.message.lightningestablish;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.LNEstablish;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishAMessage implements LNEstablish {
    public byte[] pubKeyEscape;
    public byte[] pubKeyFastEscape;
    public byte[] secretHashFastEscape;
    public byte[] revocationHash;
    public long clientAmount;
    public long serverAmount;

    public LNEstablishAMessage (byte[] pubKeyEscape, byte[] pubKeyFastEscape, byte[] secretHashFastEscape, byte[] revocationHash, long clientAmount, long
            serverAmount) {
        this.pubKeyEscape = pubKeyEscape;
        this.pubKeyFastEscape = pubKeyFastEscape;
        this.secretHashFastEscape = secretHashFastEscape;
        this.revocationHash = revocationHash;
        this.clientAmount = clientAmount;
        this.serverAmount = serverAmount;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(pubKeyEscape);
        Preconditions.checkNotNull(pubKeyFastEscape);
        Preconditions.checkNotNull(secretHashFastEscape);
        Preconditions.checkNotNull(revocationHash);
    }

    @Override
    public String toString () {
        return "LNEstablishAMessage{" +
                "serverAmount=" + serverAmount +
                ", clientAmount=" + clientAmount +
                '}';
    }
}
