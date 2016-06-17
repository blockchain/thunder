package network.thunder.core.database.hibernate;

import network.thunder.core.communication.NodeKey;
import org.bitcoinj.core.ECKey;

import javax.persistence.AttributeConverter;

/**
 * Created by Jean-Pierre Rupp on 09/06/16.
 */

public class NodeKeyConverter implements AttributeConverter<NodeKey, byte[]> {
    final private ECKeyConverter ecKeyAttributeConverter = new ECKeyConverter();

    @Override
    public byte[] convertToDatabaseColumn (NodeKey nodeKey) {
        if (nodeKey == null) {
            return null;
        } else {
            ECKey ecKey = nodeKey.unwrap();
            return ecKeyAttributeConverter.convertToDatabaseColumn(ecKey);
        }
    }

    @Override
    public NodeKey convertToEntityAttribute (byte[] bytes) {
        if (bytes == null) {
            return null;
        } else {
            ECKey ecKey = ecKeyAttributeConverter.convertToEntityAttribute(bytes);
            return ecKey == null ? null : NodeKey.wrap(ecKey);
        }
    }
}
