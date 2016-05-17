package network.thunder.core.database.objects;

import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class AddressAttributeConverter implements AttributeConverter<Address, AddressWrapper> {

    public AddressAttributeConverter () {
        System.out.println("AddressAttributeConverter.AddressAttributeConverter");
    }

    @Override
    public AddressWrapper convertToDatabaseColumn (Address address) {
        AddressWrapper wrapper = new AddressWrapper();
        wrapper.address = address.toString();
        return wrapper;
    }

    @Override
    public Address convertToEntityAttribute (AddressWrapper addressWrapper) {
        try {
            return new Address(Constants.getNetwork(), addressWrapper.address);
        } catch (AddressFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
