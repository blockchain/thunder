package network.thunder.core.communication.objects.messages.impl.message.lightningestablish;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types.LNEstablishCMessage;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishCMessageImpl implements LNEstablishCMessage {
    private byte[] signatureEscape;
    private byte[] signatureFastEscape;
    private byte[] anchorHash;

    public LNEstablishCMessageImpl (byte[] signatureEscape, byte[] signatureFastEscape, byte[] anchorHash) {
        this.signatureEscape = signatureEscape;
        this.signatureFastEscape = signatureFastEscape;
        this.anchorHash = anchorHash;
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
    public byte[] getAnchorHash () {
        return anchorHash;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(signatureEscape);
        Preconditions.checkNotNull(signatureFastEscape);
        Preconditions.checkNotNull(anchorHash);
    }
}
