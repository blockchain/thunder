package network.thunder.core.database.objects;

import network.thunder.core.communication.layer.high.RevocationHash;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class RevocationHashAttributeConverter implements AttributeConverter<RevocationHash, RevocationHashWrapper> {

    public RevocationHashAttributeConverter () {
        System.out.println("RevocationHashAttributeConverter.RevocationHashAttributeConverter");
    }

    @Override
    public RevocationHashWrapper convertToDatabaseColumn (RevocationHash revocationHash) {
        return new RevocationHashWrapper(revocationHash);
    }

    @Override
    public RevocationHash convertToEntityAttribute (RevocationHashWrapper revocationHashWrapper) {
        return new RevocationHash(
                revocationHashWrapper.depth,
                revocationHashWrapper.child,
                revocationHashWrapper.secret,
                revocationHashWrapper.secretHash
        );
    }
}
