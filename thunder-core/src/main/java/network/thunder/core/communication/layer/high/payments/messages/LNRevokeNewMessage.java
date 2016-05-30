package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.layer.high.RevocationHash;

public interface LNRevokeNewMessage {
    RevocationHash getNewRevocationHash ();
    void setNewRevocationHash (RevocationHash newRevocationHash);
}

