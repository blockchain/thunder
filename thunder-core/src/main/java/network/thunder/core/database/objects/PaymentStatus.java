package network.thunder.core.database.objects;

public enum PaymentStatus {
    UNKNOWN(0),
    SAVED(1),
    TO_BE_EMBEDDED(21),
    CURRENTLY_EMBEDDING(22),
    EMBEDDED(23),
    TO_BE_REFUNDED(31),
    CURRENTLY_REFUNDING(32),
    REFUNDED(33),
    TO_BE_REDEEMED(41),
    CURRENTLY_REDEEMING(42),
    REDEEMED(43);

    int status;

    PaymentStatus (int i) {
        this.status = i;
    }
}
