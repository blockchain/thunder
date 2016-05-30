package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.layer.high.RevocationHash;

import java.util.List;

public interface LNRevokeOldMessage {
    List<RevocationHash> getOldRevocationHash ();
    void setOldRevocationHash (List<RevocationHash> oldRevocationHash);
}

