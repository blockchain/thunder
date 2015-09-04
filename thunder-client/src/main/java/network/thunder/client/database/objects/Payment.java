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

import network.thunder.client.database.MySQLConnection;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class Payment {

	public Connection conn;
	public boolean paymentToServer;
	int id;
	int channelIdSender;
	int channelIdReceiver;
	long amount;
	/**
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
	int phase;
	String secretHash;
	String secret;
	int timestampCreated;
	int timestampSettled;
	int timestampAddedToReceiver;
	String receiver;
	boolean includeInSenderChannel;
	boolean includeInReceiverChannel;
	int settlementTxSenderID;
	Transaction settlementTxSender;
	boolean settlementTxSenderChanged;

	int settlementTxReceiverID;
	Transaction settlementTxReceiver;
	boolean settlementTxReceiverChanged;

	int refundTxSenderID;
	Transaction refundTxSender;
	boolean refundTxSenderChanged;

	int refundTxReceiverID;
	Transaction refundTxReceiver;
	boolean refundTxReceiverChanged;

	int addTxSenderID;
	Transaction addTxSender;
	boolean addTxSenderChanged;

	int addTxReceiverID;
	Transaction addTxReceiver;
	boolean addTxReceiverChanged;

	int settlementTxSenderTempID;
	Transaction settlementTxSenderTemp;
	boolean settlementTxSenderTempChanged;

	int settlementTxReceiverTempID;
	Transaction settlementTxReceiverTemp;
	boolean settlementTxReceiverTempChanged;

	int refundTxSenderTempID;
	Transaction refundTxSenderTemp;
	boolean refundTxSenderTempChanged;

	int refundTxReceiverTempID;
	Transaction refundTxReceiverTemp;
	boolean refundTxReceiverTempChanged;

	int addTxSenderTempID;
	Transaction addTxSenderTemp;
	boolean addTxSenderTempChanged;

	int addTxReceiverTempID;
	Transaction addTxReceiverTemp;
	boolean addTxReceiverTempChanged;

	public Payment (ResultSet result) throws SQLException {
		id = result.getInt("id");
		channelIdReceiver = result.getInt("channel_id_receiver");
		channelIdSender = result.getInt("channel_id_sender");
		amount = result.getLong("amount");
		phase = result.getInt("phase");
		secretHash = result.getString("secret_hash");
		secret = result.getString("secret");

		settlementTxReceiverID = result.getInt("settlement_tx_receiver");
		settlementTxSenderID = result.getInt("settlement_tx_sender");
		refundTxReceiverID = result.getInt("refund_tx_receiver");
		refundTxSenderID = result.getInt("refund_tx_sender");
		addTxReceiverID = result.getInt("add_tx_receiver");
		addTxSenderID = result.getInt("add_tx_sender");

		settlementTxReceiverTempID = result.getInt("settlement_tx_receiver_temp");
		settlementTxSenderTempID = result.getInt("settlement_tx_sender_temp");
		refundTxReceiverTempID = result.getInt("refund_tx_receiver_temp");
		refundTxSenderTempID = result.getInt("refund_tx_sender_temp");
		addTxReceiverTempID = result.getInt("add_tx_receiver_temp");
		addTxSenderTempID = result.getInt("add_tx_sender_temp");

		timestampCreated = result.getInt("timestamp_created");
		timestampSettled = result.getInt("timestamp_settled");
		timestampAddedToReceiver = result.getInt("timestamp_added_to_receiver");

		includeInReceiverChannel = Tools.intToBool(result.getInt("include_in_receiver_channel"));
		includeInSenderChannel = Tools.intToBool(result.getInt("include_in_sender_channel"));
	}

	public Payment (int channelIdSender, int channelIdReceiver, long amount, String secretHash) {
		this.channelIdReceiver = channelIdReceiver;
		this.channelIdSender = channelIdSender;
		this.amount = amount;
		this.secretHash = secretHash;
		this.timestampCreated = Tools.currentTime();
		this.includeInReceiverChannel = false;
		this.includeInSenderChannel = false;
		this.phase = 0;
	}

	public Payment (int channelIdSender, String receiver, long amount, String secretHash) {
		/**
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
		this.phase = 0;
	}

	public Payment (int channelIdSender, int channelIdReceiver, long amount) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		this.channelIdReceiver = channelIdReceiver;
		this.channelIdSender = channelIdSender;
		this.amount = amount;
		this.timestampCreated = Tools.currentTime();
		this.includeInReceiverChannel = false;
		this.includeInSenderChannel = false;
		this.phase = 0;

		byte[] b = new byte[20];
		new Random().nextBytes(b);
		secret = Tools.byteToString(b);
		secretHash = Tools.hashSecret(b);

		paymentToServer = true;

	}

	public Transaction getAddTxReceiver () throws SQLException {
		if (addTxReceiver == null) {
			addTxReceiver = MySQLConnection.getTransaction(conn, addTxReceiverID);
		}
		return addTxReceiver;
	}

	private void setAddTxReceiver (Transaction addTxReceiver) {
		addTxReceiverChanged = true;
		this.addTxReceiver = addTxReceiver;
	}

	public int getAddTxReceiverID () {
		return addTxReceiverID;
	}

	public void setAddTxReceiverID (int addTxReceiverID) {
		this.addTxReceiverID = addTxReceiverID;
	}

	public Transaction getAddTxReceiverTemp () throws SQLException {
		if (addTxReceiverTemp == null) {
			addTxReceiverTemp = MySQLConnection.getTransaction(conn, addTxReceiverTempID);
		}
		return addTxReceiverTemp;
	}

	public void setAddTxReceiverTemp (Transaction addTxReceiverTemp) {
		addTxReceiverTempChanged = true;
		this.addTxReceiverTemp = addTxReceiverTemp;
	}

	public int getAddTxReceiverTempID () {
		return addTxReceiverTempID;
	}

	public void setAddTxReceiverTempID (int addTxReceiverTempID) {
		this.addTxReceiverTempID = addTxReceiverTempID;
	}

	public Transaction getAddTxSender () throws SQLException {
		if (addTxSender == null) {
			addTxSender = MySQLConnection.getTransaction(conn, addTxSenderID);
		}
		return addTxSender;
	}

	private void setAddTxSender (Transaction addTxSender) {
		addTxSenderChanged = true;
		this.addTxSender = addTxSender;
	}

	public int getAddTxSenderID () {
		return addTxSenderID;
	}

	public void setAddTxSenderID (int addTxSenderID) {
		this.addTxSenderID = addTxSenderID;
	}

	public Transaction getAddTxSenderTemp () throws SQLException {
		if (addTxSenderTemp == null) {
			addTxSenderTemp = MySQLConnection.getTransaction(conn, addTxSenderTempID);
		}
		return addTxSenderTemp;
	}

	public void setAddTxSenderTemp (Transaction addTxSenderTemp) {
		addTxSenderTempChanged = true;
		this.addTxSenderTemp = addTxSenderTemp;
	}

	public int getAddTxSenderTempID () {
		return addTxSenderTempID;
	}

	public void setAddTxSenderTempID (int addTxSenderTempID) {
		this.addTxSenderTempID = addTxSenderTempID;
	}

	public long getAmount () {
		return amount;
	}

	public void setAmount (long amount) {
		this.amount = amount;
	}

	public int getChannelIdReceiver () {
		return channelIdReceiver;
	}

	public void setChannelIdReceiver (int channelIdReceiver) {
		this.channelIdReceiver = channelIdReceiver;
	}

	public int getChannelIdSender () {
		return channelIdSender;
	}

	public void setChannelIdSender (int channelIdSender) {
		this.channelIdSender = channelIdSender;
	}

	public Connection getConn () {
		return conn;
	}

	public void setConn (Connection conn) {
		this.conn = conn;
	}

	public int getId () {
		return id;
	}

	public void setId (int id) {
		this.id = id;
	}

	public int getPhase () {
		return phase;
	}

	public void setPhase (int phase) {
		this.phase = phase;
	}

	public String getReceiver () {
		return receiver;
	}

	public void setReceiver (String receiver) {
		this.receiver = receiver;
	}

	public Transaction getRefundTxReceiver () throws SQLException {
		if (refundTxReceiver == null) {
			refundTxReceiver = MySQLConnection.getTransaction(conn, refundTxReceiverID);
		}
		return refundTxReceiver;
	}

	private void setRefundTxReceiver (Transaction refundTxReceiver) {
		refundTxReceiverChanged = true;
		this.refundTxReceiver = refundTxReceiver;
	}

	public int getRefundTxReceiverID () {
		return refundTxReceiverID;
	}

	public void setRefundTxReceiverID (int refundTxReceiverID) {
		this.refundTxReceiverID = refundTxReceiverID;
	}

	public Transaction getRefundTxReceiverTemp () throws SQLException {
		if (refundTxReceiverTemp == null) {
			refundTxReceiverTemp = MySQLConnection.getTransaction(conn, refundTxReceiverTempID);
		}
		return refundTxReceiverTemp;
	}

	public void setRefundTxReceiverTemp (Transaction refundTxReceiverTemp) {
		refundTxReceiverTempChanged = true;
		this.refundTxReceiverTemp = refundTxReceiverTemp;
	}

	public int getRefundTxReceiverTempID () {
		return refundTxReceiverTempID;
	}

	public void setRefundTxReceiverTempID (int refundTxReceiverTempID) {
		this.refundTxReceiverTempID = refundTxReceiverTempID;
	}

	public Transaction getRefundTxSender () throws SQLException {
		if (refundTxSender == null) {
			refundTxSender = MySQLConnection.getTransaction(conn, refundTxSenderID);
		}
		return refundTxSender;
	}

	private void setRefundTxSender (Transaction refundTxSender) {
		refundTxSenderChanged = true;
		this.refundTxSender = refundTxSender;
	}

	public int getRefundTxSenderID () {
		return refundTxSenderID;
	}

	public void setRefundTxSenderID (int refundTxSenderID) {
		this.refundTxSenderID = refundTxSenderID;
	}

	public Transaction getRefundTxSenderTemp () throws SQLException {
		if (refundTxSenderTemp == null) {
			refundTxSenderTemp = MySQLConnection.getTransaction(conn, refundTxSenderTempID);
		}
		return refundTxSenderTemp;
	}

	public void setRefundTxSenderTemp (Transaction refundTxSenderTemp) {
		refundTxSenderTempChanged = true;
		this.refundTxSenderTemp = refundTxSenderTemp;
	}

	public int getRefundTxSenderTempID () {
		return refundTxSenderTempID;
	}

	public void setRefundTxSenderTempID (int refundTxSenderTempID) {
		this.refundTxSenderTempID = refundTxSenderTempID;
	}

	public String getSecret () {
		return secret;
	}

	public void setSecret (String secret) {
		this.secret = secret;
	}

	public String getSecretHash () {
		return secretHash;
	}

	public void setSecretHash (String secretHash) {
		this.secretHash = secretHash;
	}

	public Transaction getSettlementTxReceiver () throws SQLException {
		if (settlementTxReceiver == null) {
			settlementTxReceiver = MySQLConnection.getTransaction(conn, settlementTxReceiverID);
		}
		return settlementTxReceiver;
	}

	private void setSettlementTxReceiver (Transaction settlementTxReceiver) {
		settlementTxReceiverChanged = true;
		this.settlementTxReceiver = settlementTxReceiver;
	}

	public int getSettlementTxReceiverID () {
		return settlementTxReceiverID;
	}

	public void setSettlementTxReceiverID (int settlementTxReceiverID) {
		this.settlementTxReceiverID = settlementTxReceiverID;
	}

	public Transaction getSettlementTxReceiverTemp () throws SQLException {
		if (settlementTxReceiverTemp == null) {
			settlementTxReceiverTemp = MySQLConnection.getTransaction(conn, settlementTxReceiverTempID);
		}
		return settlementTxReceiverTemp;
	}

	public void setSettlementTxReceiverTemp (Transaction settlementTxReceiverTemp) {
		settlementTxReceiverTempChanged = true;
		this.settlementTxReceiverTemp = settlementTxReceiverTemp;
	}

	public int getSettlementTxReceiverTempID () {
		return settlementTxReceiverTempID;
	}

	public void setSettlementTxReceiverTempID (int settlementTxReceiverTempID) {
		this.settlementTxReceiverTempID = settlementTxReceiverTempID;
	}

	public Transaction getSettlementTxSender () throws SQLException {
		if (settlementTxSender == null) {
			settlementTxSender = MySQLConnection.getTransaction(conn, settlementTxSenderID);
		}
		return settlementTxSender;
	}

	private void setSettlementTxSender (Transaction settlementTxSender) {
		settlementTxSenderChanged = true;
		this.settlementTxSender = settlementTxSender;
	}

	public int getSettlementTxSenderID () {
		return settlementTxSenderID;
	}

	public void setSettlementTxSenderID (int settlementTxSenderID) {
		this.settlementTxSenderID = settlementTxSenderID;
	}

	public Transaction getSettlementTxSenderTemp () throws SQLException {
		if (settlementTxSenderTemp == null) {
			settlementTxSenderTemp = MySQLConnection.getTransaction(conn, settlementTxSenderTempID);
		}
		return settlementTxSenderTemp;
	}

	public void setSettlementTxSenderTemp (Transaction settlementTxSenderTemp) {
		settlementTxSenderTempChanged = true;
		this.settlementTxSenderTemp = settlementTxSenderTemp;
	}

	public int getSettlementTxSenderTempID () {
		return settlementTxSenderTempID;
	}

	public void setSettlementTxSenderTempID (int settlementTxSenderTempID) {
		this.settlementTxSenderTempID = settlementTxSenderTempID;
	}

	public int getTimestampAddedToReceiver () {
		return timestampAddedToReceiver;
	}

	public void setTimestampAddedToReceiver (int timestampAddedToReceiver) {
		this.timestampAddedToReceiver = timestampAddedToReceiver;
	}

	public int getTimestampCreated () {
		return timestampCreated;
	}

	public void setTimestampCreated (int timestampCreated) {
		this.timestampCreated = timestampCreated;
	}

	public int getTimestampSettled () {
		return timestampSettled;
	}

	public void setTimestampSettled (int timestampSettled) {
		this.timestampSettled = timestampSettled;
	}

	public boolean isIncludeInReceiverChannel () {
		return includeInReceiverChannel;
	}

	public void setIncludeInReceiverChannel (boolean includeInReceiverChannel) {
		this.includeInReceiverChannel = includeInReceiverChannel;
	}

	public boolean isIncludeInSenderChannel () {
		return includeInSenderChannel;
	}

	public void setIncludeInSenderChannel (boolean includeInSenderChannel) {
		this.includeInSenderChannel = includeInSenderChannel;
	}

	public void replaceCurrentTransactionsWithTemporary () {

		int temp1 = settlementTxReceiverID;
		int temp2 = settlementTxSenderID;
		int temp3 = refundTxReceiverID;
		int temp4 = refundTxSenderID;
		int temp5 = addTxReceiverID;
		int temp6 = addTxSenderID;

		settlementTxReceiverID = settlementTxReceiverTempID;
		settlementTxSenderID = settlementTxSenderTempID;
		refundTxReceiverID = refundTxReceiverTempID;
		refundTxSenderID = refundTxSenderTempID;
		addTxReceiverID = addTxReceiverTempID;
		addTxSenderID = addTxSenderTempID;

		settlementTxReceiverTempID = temp1;
		settlementTxSenderTempID = temp2;
		refundTxReceiverTempID = temp3;
		refundTxSenderTempID = temp4;
		addTxReceiverTempID = temp5;
		addTxSenderTempID = temp6;
	}

	@Override
	public String toString () {
		String s = "";
		String amount = Coin.valueOf(this.amount).toFriendlyString();
		if (paymentToServer) {
			s += "[ > ]		";
			s += amount;
			s += "	to	" + this.getSecretHash();
			if (this.getSecret() != null) {
				s += "	(	" + this.getSecret() + "	)";
			}
		} else {
			s += "[ < ]		";
			s += amount;
			s += "	to	" + this.getSecretHash();
			if (this.getSecret() != null) {
				s += "	(	" + this.getSecret() + "	)";
			}
		}
		return s;
	}

	public String toStringFull () {
		return "Payment{" +
				"conn=" + conn +
				", id=" + id +
				", channelIdSender=" + channelIdSender +
				", channelIdReceiver=" + channelIdReceiver +
				", amount=" + amount +
				", phase=" + phase +
				", secretHash='" + secretHash + '\'' +
				", secret='" + secret + '\'' +
				", timestampCreated=" + timestampCreated +
				", timestampSettled=" + timestampSettled +
				", timestampAddedToReceiver=" + timestampAddedToReceiver +
				", receiver='" + receiver + '\'' +
				", includeInSenderChannel=" + includeInSenderChannel +
				", includeInReceiverChannel=" + includeInReceiverChannel +
				", paymentToServer=" + paymentToServer +
				", settlementTxSenderID=" + settlementTxSenderID +
				", settlementTxSender=" + settlementTxSender +
				", settlementTxSenderChanged=" + settlementTxSenderChanged +
				", settlementTxReceiverID=" + settlementTxReceiverID +
				", settlementTxReceiver=" + settlementTxReceiver +
				", settlementTxReceiverChanged=" + settlementTxReceiverChanged +
				", refundTxSenderID=" + refundTxSenderID +
				", refundTxSender=" + refundTxSender +
				", refundTxSenderChanged=" + refundTxSenderChanged +
				", refundTxReceiverID=" + refundTxReceiverID +
				", refundTxReceiver=" + refundTxReceiver +
				", refundTxReceiverChanged=" + refundTxReceiverChanged +
				", addTxSenderID=" + addTxSenderID +
				", addTxSender=" + addTxSender +
				", addTxSenderChanged=" + addTxSenderChanged +
				", addTxReceiverID=" + addTxReceiverID +
				", addTxReceiver=" + addTxReceiver +
				", addTxReceiverChanged=" + addTxReceiverChanged +
				", settlementTxSenderTempID=" + settlementTxSenderTempID +
				", settlementTxSenderTemp=" + settlementTxSenderTemp +
				", settlementTxSenderTempChanged=" + settlementTxSenderTempChanged +
				", settlementTxReceiverTempID=" + settlementTxReceiverTempID +
				", settlementTxReceiverTemp=" + settlementTxReceiverTemp +
				", settlementTxReceiverTempChanged=" + settlementTxReceiverTempChanged +
				", refundTxSenderTempID=" + refundTxSenderTempID +
				", refundTxSenderTemp=" + refundTxSenderTemp +
				", refundTxSenderTempChanged=" + refundTxSenderTempChanged +
				", refundTxReceiverTempID=" + refundTxReceiverTempID +
				", refundTxReceiverTemp=" + refundTxReceiverTemp +
				", refundTxReceiverTempChanged=" + refundTxReceiverTempChanged +
				", addTxSenderTempID=" + addTxSenderTempID +
				", addTxSenderTemp=" + addTxSenderTemp +
				", addTxSenderTempChanged=" + addTxSenderTempChanged +
				", addTxReceiverTempID=" + addTxReceiverTempID +
				", addTxReceiverTemp=" + addTxReceiverTemp +
				", addTxReceiverTempChanged=" + addTxReceiverTempChanged +
				'}';
	}

	public void updateTransactionsToDatabase (Connection conn) throws SQLException {
		if (settlementTxSenderChanged) {
			settlementTxSenderID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(settlementTxSender, channelIdSender,
					settlementTxSenderID));
		}
		if (settlementTxReceiverChanged) {
			settlementTxReceiverID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(settlementTxReceiver, channelIdReceiver,
					settlementTxReceiverID));
		}
		if (refundTxSenderChanged) {
			refundTxSenderID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxSender, channelIdSender, refundTxSenderID));
		}
		if (refundTxReceiverChanged) {
			refundTxReceiverID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxReceiver, channelIdReceiver, refundTxReceiverID));
		}
		if (addTxSenderChanged) {
			addTxSenderID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(addTxSender, channelIdSender, addTxSenderID));
		}
		if (addTxReceiverChanged) {
			addTxReceiverID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(addTxReceiver, channelIdReceiver, addTxReceiverID));
		}
		if (settlementTxSenderTempChanged) {
			settlementTxSenderTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(settlementTxSenderTemp, channelIdSender,
					settlementTxSenderTempID));
		}
		if (settlementTxReceiverTempChanged) {
			settlementTxReceiverTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(settlementTxReceiverTemp, channelIdReceiver,
					settlementTxReceiverTempID));
		}
		if (refundTxSenderTempChanged) {
			refundTxSenderTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxSenderTemp, channelIdSender,
					refundTxSenderTempID));
		}
		if (refundTxReceiverTempChanged) {
			refundTxReceiverTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxReceiverTemp, channelIdReceiver,
					refundTxReceiverTempID));
		}
		if (addTxSenderTempChanged) {
			addTxSenderTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(addTxSenderTemp, channelIdSender, addTxSenderTempID));
		}
		if (addTxReceiverTempChanged) {
			addTxReceiverTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(addTxReceiverTemp, channelIdReceiver,
					addTxReceiverTempID));
		}

	}
}
