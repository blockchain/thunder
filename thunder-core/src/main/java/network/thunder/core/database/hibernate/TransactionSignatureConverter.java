package network.thunder.core.database.hibernate;

import org.bitcoinj.crypto.TransactionSignature;

import javax.persistence.AttributeConverter;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */
public class TransactionSignatureConverter implements AttributeConverter<TransactionSignature, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn (TransactionSignature transactionSignature) {
        return transactionSignature == null ? null : transactionSignature.encodeToBitcoin();
    }

    @Override
    public TransactionSignature convertToEntityAttribute (byte[] bytes) {
        return bytes == null ? null : TransactionSignature.decodeFromBitcoin(bytes, true, true);
    }
}
