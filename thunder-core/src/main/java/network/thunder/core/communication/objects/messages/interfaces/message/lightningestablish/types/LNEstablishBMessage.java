package network.thunder.core.communication.objects.messages.interfaces.message.lightningestablish.types;

import network.thunder.core.communication.Message;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public interface LNEstablishBMessage extends Message {
    byte[] getPubKeyEscape ();

    byte[] getPubKeyFastEscape ();

    byte[] getRevocationHash ();

    byte[] getSecretHashFastEscape ();

    byte[] getAnchorHash ();

    long getServerAmount ();
}
