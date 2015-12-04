package network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types;

import network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.LNEstablish;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public interface LNEstablishDMessage extends LNEstablish {
    byte[] getSignatureEscape ();

    byte[] getSignatureFastEscape ();
}
