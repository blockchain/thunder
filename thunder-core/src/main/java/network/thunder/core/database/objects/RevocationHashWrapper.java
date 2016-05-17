package network.thunder.core.database.objects;

import network.thunder.core.communication.layer.high.RevocationHash;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class RevocationHashWrapper {
    @Id
    @GeneratedValue
    private int id;

    public int depth;
    public int child;
    public byte[] secret;
    public byte[] secretHash;

    public RevocationHashWrapper () {
    }

    public RevocationHashWrapper (RevocationHash revocationHash) {
        this.depth = revocationHash.getDepth();
        this.child = revocationHash.getChild();
        this.secret = revocationHash.getSecret();
        this.secretHash = revocationHash.getSecretHash();
    }
}
