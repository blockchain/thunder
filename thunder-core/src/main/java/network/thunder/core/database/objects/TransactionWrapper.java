package network.thunder.core.database.objects;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TransactionWrapper {
    @Id
    public byte[] hash;

    public byte[] payload;

    public TransactionWrapper () {
    }
}
