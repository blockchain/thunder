package network.thunder.core.communication.objects.messages.impl.message.lightningestablish;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.LNEstablish;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishCMessage implements LNEstablish {
    public byte[] signatureEscape;
    public byte[] signatureFastEscape;
    public byte[] anchorHash;

    public LNEstablishCMessage (byte[] signatureEscape, byte[] signatureFastEscape, byte[] anchorHash) {
        this.signatureEscape = signatureEscape;
        this.signatureFastEscape = signatureFastEscape;
        this.anchorHash = anchorHash;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(signatureEscape);
        Preconditions.checkNotNull(signatureFastEscape);
        Preconditions.checkNotNull(anchorHash);
    }
}
