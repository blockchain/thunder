package network.thunder.core.communication.objects.messages.impl.message.lightningestablish;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types.LNEstablishBMessage;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishBMessageImpl implements LNEstablishBMessage {
    private byte[] pubKeyEscape;
    private byte[] pubKeyFastEscape;
    private byte[] secretHashFastEscape;
    private byte[] revocationHash;
    private byte[] anchorHash;
    private long serverAmount;

    public LNEstablishBMessageImpl (byte[] pubKeyEscape, byte[] pubKeyFastEscape, byte[] secretHashFastEscape, byte[] revocationHash, byte[] anchorHash, long
            serverAmount) {
        this.pubKeyEscape = pubKeyEscape;
        this.pubKeyFastEscape = pubKeyFastEscape;
        this.secretHashFastEscape = secretHashFastEscape;
        this.revocationHash = revocationHash;
        this.anchorHash = anchorHash;
        this.serverAmount = serverAmount;
    }

    @Override
    public byte[] getPubKeyEscape () {
        return pubKeyEscape;
    }

    @Override
    public byte[] getPubKeyFastEscape () {
        return pubKeyFastEscape;
    }

    @Override
    public byte[] getRevocationHash () {
        return revocationHash;
    }

    @Override
    public byte[] getSecretHashFastEscape () {
        return secretHashFastEscape;
    }

    @Override
    public byte[] getAnchorHash () {
        return anchorHash;
    }

    @Override
    public long getServerAmount () {
        return serverAmount;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(pubKeyEscape);
        Preconditions.checkNotNull(pubKeyFastEscape);
        Preconditions.checkNotNull(secretHashFastEscape);
        Preconditions.checkNotNull(revocationHash);
        Preconditions.checkNotNull(anchorHash);
    }
}
