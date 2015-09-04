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
package network.thunder.server.database.objects;

import network.thunder.server.etc.Constants;
import network.thunder.server.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.spongycastle.util.encoders.Base64;

import java.sql.ResultSet;
import java.sql.SQLException;

// TODO: Auto-generated Javadoc

/**
 * The Class Output.
 */
public class Output {

	/**
	 * The hash.
	 */
	String hash;

	/**
	 * The vout.
	 */
	int vout;

	/**
	 * The value.
	 */
	long value;

	/**
	 * The private key.
	 */
	String privateKey;

	/**
	 * The lock.
	 */
	int lock;

	/**
	 * The channel id.
	 */
	int channelId;

	/**
	 * The transaction output.
	 */
	TransactionOutput transactionOutput;
	/**
	 * The key.
	 */
	ECKey key;

	/**
	 * Instantiates a new output.
	 */
	public Output () {
	}

	/**
	 * Instantiates a new output.
	 *
	 * @param results the results
	 * @throws SQLException the SQL exception
	 */
	public Output (ResultSet results) throws SQLException {
		setHash(results.getString("transaction_hash"));
		setVout(results.getInt("vout"));
		setValue(results.getLong("value"));
		setPrivateKey(results.getString("private_key"));
		setLock(results.getInt("timestamp_locked"));
		setTransactionOutput(new TransactionOutput(Constants.getNetwork(), null, Tools.stringToByte(results.getString("transaction_output")), 0));
	}

	/**
	 * Instantiates a new output.
	 *
	 * @param o      the o
	 * @param wallet the wallet
	 */
	public Output (TransactionOutput o, Wallet wallet) {
		setVout(o.getIndex());
		setHash(o.getParentTransaction().getHash().toString());
		setValue(o.getValue().value);
		setPrivateKey(new String(Base64.encode(wallet.findKeyFromPubHash(o.getAddressFromP2PKHScript(Constants.getNetwork()).getHash160()).getPrivKeyBytes()
		)));
		setTransactionOutput(o);
	}

	/**
	 * Gets the channel id.
	 *
	 * @return the channel id
	 */
	public int getChannelId () {
		return channelId;
	}

	/**
	 * Sets the channel id.
	 *
	 * @param channelId the new channel id
	 */
	public void setChannelId (int channelId) {
		this.channelId = channelId;
	}

	/**
	 * Gets the channel pub key.
	 *
	 * @return the channel pub key
	 */
	public int getChannelPubKey () {
		return channelId;
	}

	/**
	 * Sets the channel pub key.
	 *
	 * @param channelId the new channel pub key
	 */
	public void setChannelPubKey (int channelId) {
		this.channelId = channelId;
	}

	/**
	 * Gets the EC key.
	 *
	 * @return the EC key
	 */
	public ECKey getECKey () {
		if (key == null) {
			key = ECKey.fromPrivate(Base64.decode(privateKey));
		}
		return key;
	}

	/**
	 * Gets the hash.
	 *
	 * @return the hash
	 */
	public String getHash () {
		return hash;
	}

	/**
	 * Sets the hash.
	 *
	 * @param hash the new hash
	 */
	public void setHash (String hash) {
		this.hash = hash;
	}

	/**
	 * Gets the lock.
	 *
	 * @return the lock
	 */
	public int getLock () {
		return lock;
	}

	/**
	 * Sets the lock.
	 *
	 * @param lock the new lock
	 */
	public void setLock (int lock) {
		this.lock = lock;
	}

	/**
	 * Gets the private key.
	 *
	 * @return the private key
	 */
	public String getPrivateKey () {
		return privateKey;
	}

	/**
	 * Sets the private key.
	 *
	 * @param privateKey the new private key
	 */
	public void setPrivateKey (String privateKey) {
		this.privateKey = privateKey;
	}

	/**
	 * Gets the transaction output.
	 *
	 * @return the transaction output
	 */
	public TransactionOutput getTransactionOutput () {
		return transactionOutput;
	}

	/**
	 * Sets the transaction output.
	 *
	 * @param transactionOutput the new transaction output
	 */
	public void setTransactionOutput (TransactionOutput transactionOutput) {
		this.transactionOutput = transactionOutput;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public long getValue () {
		return value;
	}

	/**
	 * Sets the value.
	 *
	 * @param value the new value
	 */
	public void setValue (long value) {
		this.value = value;
	}

	/**
	 * Gets the vout.
	 *
	 * @return the vout
	 */
	public int getVout () {
		return vout;
	}

	/**
	 * Sets the vout.
	 *
	 * @param vout the new vout
	 */
	public void setVout (int vout) {
		this.vout = vout;
	}

}
