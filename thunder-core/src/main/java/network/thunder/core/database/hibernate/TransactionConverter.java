package network.thunder.core.database.hibernate;

import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Transaction;

import javax.persistence.AttributeConverter;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

public class TransactionConverter implements AttributeConverter<Transaction, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn (Transaction transaction) {
        return transaction == null ? null : transaction.bitcoinSerialize();
    }

    @Override
    public Transaction convertToEntityAttribute (byte[] bytes) {
        return bytes == null ? null : new Transaction(Constants.getNetwork(), bytes);
    }
}
