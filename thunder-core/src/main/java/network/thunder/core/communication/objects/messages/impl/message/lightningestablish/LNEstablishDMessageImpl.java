package network.thunder.core.communication.objects.messages.impl.message.lightningestablish;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types.LNEstablishDMessage;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishDMessageImpl implements LNEstablishDMessage {
    private byte[] signatureEscape;
    private byte[] signatureFastEscape;

    public LNEstablishDMessageImpl (byte[] signatureEscape, byte[] signatureFastEscape) {
        this.signatureEscape = signatureEscape;
        this.signatureFastEscape = signatureFastEscape;
    }

    @Override
    public byte[] getSignatureEscape () {
        return signatureEscape;
    }

    @Override
    public byte[] getSignatureFastEscape () {
        return signatureFastEscape;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(signatureEscape);
        Preconditions.checkNotNull(signatureFastEscape);
    }
}
