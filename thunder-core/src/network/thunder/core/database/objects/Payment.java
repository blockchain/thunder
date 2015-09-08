/*
 * ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 * Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package network.thunder.core.database.objects;

import network.thunder.core.etc.Tools;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

// TODO: Auto-generated Javadoc

/**
 * The Class Payment.
 */
public class Payment {

	private String receiver;
	/**
	 * The conn.
	 */
	public Connection conn;
	/**
	 * The payment to server.
	 */
	public boolean paymentToServer;
	/**
	 * The id.
	 */
	int id;
	/**
	 * The channel id sender.
	 */
	int channelIdSender;
	/**
	 * The channel id receiver.
	 */
	int channelIdReceiver;
	/**
	 * The amount.
	 */
	long amount;
	/*
	 * Different phases of a payment:
	 * <p>
	 * 0 - sender requested payment
	 * 1 - payment request complete - include in sender channel
	 * also add it to the receivers channel next time..
	 * <p>
	 * 2 -
	 * 3 - receiver channel updated, include in both channels
	 * 4 - receiver released the secret
	 * <p>
	 * 10 - settled with sender only
	 * 5 - settled with receiver only
	 * 11 - payment settled
	 * 5 - receiver/server requested refund
	 * 6 - receiver refunded/timeouted
	 * 12 - receiver and sender refunded (so it's settled aswell..)
	 */

	int phaseReceiver;
	int phaseSender;
	long fee;
	/**
	 * The secret hash.
	 */
	String secretHash;
	/**
	 * The secret.
	 */
	String secret;
	/**
	 * The timestamp created.
	 */
	int timestampCreated;
	/**
	 * The timestamp settled.
	 */
	int timestampSettledReceiver;
	int timestampSettledSender;
	/**
	 * The timestamp added to receiver.
	 */
	int timestampAddedToReceiver;

	boolean includeInSenderChannel;
	boolean includeInReceiverChannel;
	boolean includeInReceiverChannelTemp;
	boolean includeInSenderChannelTemp;

	/**
	 * Instantiates a new payment.
	 *
	 * @param result the result
	 * @throws SQLException the SQL exception
	 */
	public Payment (ResultSet result) throws SQLException {
		id = result.getInt("id");
		channelIdReceiver = result.getInt("channel_id_receiver");
		channelIdSender = result.getInt("channel_id_sender");
		amount = result.getLong("amount");
		fee = result.getLong("fee");
		phaseSender = result.getInt("phase_sender");
		phaseReceiver = result.getInt("phase_receiver");
		secretHash = result.getString("secret_hash");
		secret = result.getString("secret");

		timestampCreated = result.getInt("timestamp_created");
		timestampSettledReceiver = result.getInt("timestamp_settled_receiver");
		timestampSettledSender = result.getInt("timestamp_settled_sender");
		timestampAddedToReceiver = result.getInt("timestamp_added_to_receiver");

		includeInReceiverChannel = Tools.intToBool(result.getInt("include_in_receiver_channel"));
		includeInSenderChannel = Tools.intToBool(result.getInt("include_in_sender_channel"));

		includeInReceiverChannelTemp = Tools.intToBool(result.getInt("include_in_receiver_channel_temp"));
		includeInSenderChannelTemp = Tools.intToBool(result.getInt("include_in_sender_channel_temp"));
	}

	/**
	 * Instantiates a new payment.
	 *
	 * @param channelIdSender   the channel id sender
	 * @param channelIdReceiver the channel id receiver
	 * @param amount            the amount
	 * @param secretHash        the secret hash
	 */
	public Payment (int channelIdSender, int channelIdReceiver, long amount, String secretHash) {
		this.channelIdReceiver = channelIdReceiver;
		this.channelIdSender = channelIdSender;
		this.amount = amount;
		this.secretHash = secretHash;
		this.timestampCreated = Tools.currentTime();
		this.includeInReceiverChannel = false;
		this.includeInSenderChannel = false;
		this.phaseSender = 0;
		this.phaseReceiver = 0;

		this.fee = Tools.calculateServerFee(this.amount);
	}

	/**
	 * Instantiates a new payment.
	 *
	 * @param channelIdSender the channel id sender
	 * @param receiver        the receiver
	 * @param amount          the amount
	 * @param secretHash      the secret hash
	 */
	public Payment (int channelIdSender, String receiver, long amount, String secretHash) {
		/*
		 * TODO: I don't know yet, if we want to store the receiver string on the client.
		 * Doing so would require a 'fork' between the two database models, as we really don't need it
		 * in the payment table on the server..
		 */
		this.channelIdSender = channelIdSender;
		this.receiver = receiver;
		this.amount = amount;
		this.secretHash = secretHash;
		this.timestampCreated = Tools.currentTime();
		this.includeInReceiverChannel = false;
		this.includeInSenderChannel = false;
		this.phaseSender = 0;
		this.phaseReceiver = 0;
	}

	/**
	 * Instantiates a new payment.
	 *
	 * @param channelIdSender   the channel id sender
	 * @param channelIdReceiver the channel id receiver
	 * @param amount            the amount
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws NoSuchAlgorithmException     the no such algorithm exception
	 */
	public Payment (int channelIdSender, int channelIdReceiver, long amount) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		this.channelIdReceiver = channelIdReceiver;
		this.channelIdSender = channelIdSender;
		this.amount = amount;
		this.timestampCreated = Tools.currentTime();
		this.includeInReceiverChannel = false;
		this.includeInSenderChannel = false;
		this.phaseSender = 0;
		this.phaseReceiver = 0;

		byte[] b = new byte[20];
		new Random().nextBytes(b);
		secret = Tools.byteToString(b);
		secretHash = Tools.hashSecret(b);

		paymentToServer = true;

	}

	/**
	 * Gets the amount.
	 *
	 * @return the amount
	 */
	public long getAmount () {
		return amount;
	}

	/**
	 * Sets the amount.
	 *
	 * @param amount the new amount
	 */
	public void setAmount (long amount) {
		this.amount = amount;
	}

	/**
	 * Gets the channel id receiver.
	 *
	 * @return the channel id receiver
	 */
	public int getChannelIdReceiver () {
		return channelIdReceiver;
	}

	/**
	 * Sets the channel id receiver.
	 *
	 * @param channelIdReceiver the new channel id receiver
	 */
	public void setChannelIdReceiver (int channelIdReceiver) {
		this.channelIdReceiver = channelIdReceiver;
	}

	/**
	 * Gets the channel id sender.
	 *
	 * @return the channel id sender
	 */
	public int getChannelIdSender () {
		return channelIdSender;
	}

	/**
	 * Sets the channel id sender.
	 *
	 * @param channelIdSender the new channel id sender
	 */
	public void setChannelIdSender (int channelIdSender) {
		this.channelIdSender = channelIdSender;
	}

	/**
	 * Gets the conn.
	 *
	 * @return the conn
	 */
	public Connection getConn () {
		return conn;
	}

	/**
	 * Sets the conn.
	 *
	 * @param conn the new conn
	 */
	public void setConn (Connection conn) {
		this.conn = conn;
	}

	public long getFee () {
		return fee;
	}

	public void setFee (long fee) {
		this.fee = fee;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public int getId () {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId (int id) {
		this.id = id;
	}

	public int getPhase () {
		if (paymentToServer) {
			return this.phaseSender;
		} else {
			return this.phaseReceiver;
		}
	}

	public void setPhase (int phase) {
		if (paymentToServer) {
			this.phaseSender = phase;
		} else {
			this.phaseReceiver = phase;
		}
	}

	public int getPhaseReceiver () {
		return phaseReceiver;
	}

	public void setPhaseReceiver (int phaseReceiver) {
		this.phaseReceiver = phaseReceiver;
	}

	public int getPhaseSender () {
		return phaseSender;
	}

	public void setPhaseSender (int phaseSender) {
		this.phaseSender = phaseSender;
	}

	/**
	 * Gets the receiver.
	 *
	 * @return the receiver
	 */
	public String getReceiver () {
		return receiver;
	}

	/**
	 * Sets the receiver.
	 *
	 * @param receiver the new receiver
	 */
	public void setReceiver (String receiver) {
		this.receiver = receiver;
	}

	/**
	 * Gets the secret.
	 *
	 * @return the secret
	 */
	public String getSecret () {
		return secret;
	}

	/**
	 * Sets the secret.
	 *
	 * @param secret the new secret
	 */
	public void setSecret (String secret) {
		this.secret = secret;
	}

	/**
	 * Gets the secret hash.
	 *
	 * @return the secret hash
	 */
	public String getSecretHash () {
		return secretHash;
	}

	/**
	 * Sets the secret hash.
	 *
	 * @param secretHash the new secret hash
	 */
	public void setSecretHash (String secretHash) {
		this.secretHash = secretHash;
	}

	/**
	 * Gets the timestamp added to receiver.
	 *
	 * @return the timestamp added to receiver
	 */
	public int getTimestampAddedToReceiver () {
		return timestampAddedToReceiver;
	}

	/**
	 * Sets the timestamp added to receiver.
	 *
	 * @param timestampAddedToReceiver the new timestamp added to receiver
	 */
	public void setTimestampAddedToReceiver (int timestampAddedToReceiver) {
		this.timestampAddedToReceiver = timestampAddedToReceiver;
	}

	/**
	 * Gets the timestamp created.
	 *
	 * @return the timestamp created
	 */
	public int getTimestampCreated () {
		return timestampCreated;
	}

	/**
	 * Sets the timestamp created.
	 *
	 * @param timestampCreated the new timestamp created
	 */
	public void setTimestampCreated (int timestampCreated) {
		this.timestampCreated = timestampCreated;
	}

	public int getTimestampSettledReceiver () {
		return timestampSettledReceiver;
	}

	public void setTimestampSettledReceiver (int timestampSettledReceiver) {
		this.timestampSettledReceiver = timestampSettledReceiver;
	}

	public int getTimestampSettledSender () {
		return timestampSettledSender;
	}

	public void setTimestampSettledSender (int timestampSettledSender) {
		this.timestampSettledSender = timestampSettledSender;
	}

	/**
	 * Checks if is include in receiver channel.
	 *
	 * @return true, if is include in receiver channel
	 */
	public boolean isIncludeInReceiverChannel () {
		return includeInReceiverChannel;
	}

	/**
	 * Sets the include in receiver channel.
	 *
	 * @param includeInReceiverChannel the new include in receiver channel
	 */
	public void setIncludeInReceiverChannel (boolean includeInReceiverChannel) {
		this.includeInReceiverChannel = includeInReceiverChannel;
	}

	public boolean isIncludeInReceiverChannelTemp () {
		return includeInReceiverChannelTemp;
	}

	public void setIncludeInReceiverChannelTemp (boolean includeInReceiverChannelTemp) {
		this.includeInReceiverChannelTemp = includeInReceiverChannelTemp;
	}

	/**
	 * Checks if is include in sender channel.
	 *
	 * @return true, if is include in sender channel
	 */
	public boolean isIncludeInSenderChannel () {
		return includeInSenderChannel;
	}

	/**
	 * Sets the include in sender channel.
	 *
	 * @param includeInSenderChannel the new include in sender channel
	 */
	public void setIncludeInSenderChannel (boolean includeInSenderChannel) {
		this.includeInSenderChannel = includeInSenderChannel;
	}

	public boolean isIncludeInSenderChannelTemp () {
		return includeInSenderChannelTemp;
	}

	public void setIncludeInSenderChannelTemp (boolean includeInSenderChannelTemp) {
		this.includeInSenderChannelTemp = includeInSenderChannelTemp;
	}

	public void setIncludedInChannel (boolean includedInChannelTemp) {
		if (paymentToServer) {
			setIncludeInSenderChannel(includedInChannelTemp);
		} else {
			setIncludeInReceiverChannel(includedInChannelTemp);
		}
	}

	public void setIncludedInChannelTemp (boolean includedInChannelTemp) {
		if (paymentToServer) {
			setIncludeInSenderChannelTemp(includedInChannelTemp);
		} else {
			setIncludeInReceiverChannelTemp(includedInChannelTemp);
		}
	}
}
