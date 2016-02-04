package network.thunder.core.database.objects;

/**
 * Created by matsjerratsch on 04/02/2016.
 */
public enum PaymentStatus {
    UNKNOWN(0),
    SAVED(1),
    EMBEDDED(2),
    REFUNDED(3),
    REDEEMED(4);

    int status;

    PaymentStatus (int i) {
        this.status = i;
    }
}
