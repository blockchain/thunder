package network.thunder.core.database.objects;

import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Transaction;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class TransactionAttributeConverter implements AttributeConverter<Transaction, TransactionWrapper> {

    public TransactionAttributeConverter () {
        System.out.println("TransactionAttributeConverter.TransactionAttributeConverter");
    }

    @Override
    public TransactionWrapper convertToDatabaseColumn (Transaction transaction) {
        System.out.println("TransactionAttributeConverter.convertToDatabaseColumn");
        TransactionWrapper wrapper = new TransactionWrapper();
        wrapper.hash = transaction.getHash().getBytes();
        wrapper.payload = transaction.bitcoinSerialize();
        return wrapper;
    }

    @Override
    public Transaction convertToEntityAttribute (TransactionWrapper transactionWrapper) {
        System.out.println("TransactionAttributeConverter.convertToEntityAttribute");
        return new Transaction(Constants.getNetwork(), transactionWrapper.payload);
    }
}
