package network.thunder.core.database.objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class AddressWrapper {
    @Id
    @GeneratedValue
    private int id;

    String address;

    public AddressWrapper () {
    }

}
