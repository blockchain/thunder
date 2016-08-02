package network.thunder.core.database.persistent.mapper;

import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Address;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AddressColumnMapper implements ResultColumnMapper<Address> {
    public static AddressColumnMapper INSTANCE = new AddressColumnMapper();

    @Override
    public Address mapColumn (ResultSet resultSet, int i, StatementContext statementContext) throws SQLException {
        return getAddress(resultSet.getString(i));
    }

    @Override
    public Address mapColumn (ResultSet resultSet, String s, StatementContext statementContext) throws SQLException {
        return getAddress(resultSet.getString(s));
    }

    public static Address getAddress(String address) {
        return new Address(Constants.getNetwork(), address);
    }
}
