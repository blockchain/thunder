/*
 *  ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 *  Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package network.thunder.client.database.objects;

import network.thunder.client.etc.Constants;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.spongycastle.util.encoders.Base64;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Output {
    String hash;
    int vout;
    long value;
    String privateKey;
    int lock;
    int channelId;
    TransactionOutput transactionOutput;
    ECKey key;

    public Output () {
    }

    public Output (ResultSet results) throws SQLException {
        setHash(results.getString("transaction_hash"));
        setVout(results.getInt("vout"));
        setValue(results.getLong("value"));
        setPrivateKey(results.getString("private_key"));
        setLock(results.getInt("timestamp_locked"));
        setTransactionOutput(new TransactionOutput(Constants.getNetwork(), null, Tools.stringToByte(results.getString("transaction_output")), 0));
    }

    public Output (TransactionOutput o, Wallet wallet) {
        setVout(o.getIndex());
        setHash(o.getParentTransaction().getHash().toString());
        setValue(o.getValue().value);
        setPrivateKey(new String(Base64.encode(wallet.findKeyFromPubHash(o.getAddressFromP2PKHScript(Constants.getNetwork()).getHash160()).getPrivKeyBytes())));
        setTransactionOutput(o);
    }

    public int getChannelId () {
        return channelId;
    }

    public void setChannelId (int channelId) {
        this.channelId = channelId;
    }

    public int getChannelPubKey () {
        return channelId;
    }

    public void setChannelPubKey (int channelId) {
        this.channelId = channelId;
    }

    public ECKey getECKey () {
        if (key == null) {
            key = ECKey.fromPrivate(Base64.decode(privateKey));
        }
        return key;
    }

    public String getHash () {
        return hash;
    }

    public void setHash (String hash) {
        this.hash = hash;
    }

    public int getLock () {
        return lock;
    }

    public void setLock (int lock) {
        this.lock = lock;
    }

    public String getPrivateKey () {
        return privateKey;
    }

    public void setPrivateKey (String privateKey) {
        this.privateKey = privateKey;
    }

    public TransactionOutput getTransactionOutput () {
        return transactionOutput;
    }

    public void setTransactionOutput (TransactionOutput transactionOutput) {
        this.transactionOutput = transactionOutput;
    }

    public long getValue () {
        return value;
    }

    public void setValue (long value) {
        this.value = value;
    }

    public int getVout () {
        return vout;
    }

    public void setVout (int vout) {
        this.vout = vout;
    }

}
