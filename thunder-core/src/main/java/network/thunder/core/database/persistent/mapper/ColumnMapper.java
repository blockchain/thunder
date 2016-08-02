package network.thunder.core.database.persistent.mapper;

import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ColumnMapper {
    public static Transaction getTransaction (byte[] tx) {
        return new Transaction(Constants.getNetwork(), tx);
    }

    public static ECKey getKey(byte[] key) {
        try {
            return ECKey.fromPrivate(key);
        } catch (Exception e) {
            return ECKey.fromPublicOnly(key);
        }
    }

    public static Transaction mapTransaction (ResultSet r, String columnLabel) throws SQLException {
        return getTransaction(r.getBytes(columnLabel));
    }

    public static ECKey mapECKey (ResultSet resultSet, String s) throws SQLException {
        return getKey(resultSet.getBytes(s));
    }

    public Address mapAddress (ResultSet resultSet, String s) throws SQLException {
        return getAddress(resultSet.getString(s));
    }

    public static Address getAddress(String address) {
        return new Address(Constants.getNetwork(), address);
    }

    public Address map (ResultSet resultSet, String s) throws SQLException {
        return getAddress(resultSet.getString(s));
    }
}
