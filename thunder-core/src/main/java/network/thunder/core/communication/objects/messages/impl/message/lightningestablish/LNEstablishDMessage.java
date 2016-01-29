package network.thunder.core.communication.objects.messages.impl.message.lightningestablish;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.LNEstablish;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishDMessage implements LNEstablish {
    public byte[] signatureEscape;
    public byte[] signatureFastEscape;

    public LNEstablishDMessage (byte[] signatureEscape, byte[] signatureFastEscape) {
        this.signatureEscape = signatureEscape;
        this.signatureFastEscape = signatureFastEscape;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(signatureEscape);
        Preconditions.checkNotNull(signatureFastEscape);
    }

    @Override
    public String toString () {
        return "LNEstablishDMessage{}";
    }
}
