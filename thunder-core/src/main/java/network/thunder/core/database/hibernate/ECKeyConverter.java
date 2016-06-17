package network.thunder.core.database.hibernate;

import org.bitcoinj.core.ECKey;

import javax.persistence.AttributeConverter;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */
public class ECKeyConverter implements AttributeConverter<ECKey, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn (ECKey ecKey) {
        if (ecKey == null) {
            return null;
        } else if (ecKey.hasPrivKey()) {
                return ecKey.getPrivKeyBytes();
        } else {
            return ecKey.getPubKey();
        }
    }

    @Override
    public ECKey convertToEntityAttribute (byte[] bytes) {
        if (bytes == null) {
            return null;
        } else if (bytes.length == 32) {
            return ECKey.fromPrivate(bytes);
        } else {
            return ECKey.fromPublicOnly(bytes);
        }
    }
}
