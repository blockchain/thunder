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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import network.thunder.server.database.MySQLConnection;
import network.thunder.server.etc.Tools;

import org.bitcoinj.core.Transaction;

// TODO: Auto-generated Javadoc
/**
 * The Class Payment.
 */
public class Payment {
	
	/**
	 * The conn.
	 */
	public Connection conn;

	
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
	
	/**
	 * Different phases of a payment:
	 * 
	 * 		0 - sender requested payment
	 * 		1 - payment request complete - include in sender channel
	 * 				also add it to the receivers channel next time..
	 * 
	 * 		2 - 
	 * 		3 - receiver channel updated, include in both channels
	 * 		4 - receiver released the secret
	 * 
	 * 		10 - settled with sender only
	 * 		5 - settled with receiver only
	 * 		11 - payment settled
	 * 		5 - receiver/server requested refund
	 * 		6 - receiver refunded/timeouted
	 * 		12 - receiver and sender refunded (so it's settled aswell..)
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
	
	/**
	 * The receiver.
	 */
	String receiver;
	
	/**
	 * The include in sender channel.
	 */
	boolean includeInSenderChannel;
	
	/**
	 * The include in receiver channel.
	 */
	boolean includeInReceiverChannel;

    boolean includeInReceiverChannelTemp;
    boolean includeInSenderChannelTemp;

	/**
	 * The payment to server.
	 */
	public boolean paymentToServer;
	

	/**
	 * The settlement tx sender id.
	 */
	int settlementTxSenderID;
	
	/**
	 * The settlement tx sender.
	 */
	Transaction settlementTxSender;
	
	/**
	 * The settlement tx sender changed.
	 */
	boolean settlementTxSenderChanged;
	
	/**
	 * The settlement tx receiver id.
	 */
	int settlementTxReceiverID;
	
	/**
	 * The settlement tx receiver.
	 */
	Transaction settlementTxReceiver;
	
	/**
	 * The settlement tx receiver changed.
	 */
	boolean settlementTxReceiverChanged;
	

	/**
	 * The refund tx sender id.
	 */
	int refundTxSenderID;
	
	/**
	 * The refund tx sender.
	 */
	Transaction refundTxSender;
	
	/**
	 * The refund tx sender changed.
	 */
	boolean refundTxSenderChanged;

	/**
	 * The refund tx receiver id.
	 */
	int refundTxReceiverID;
	
	/**
	 * The refund tx receiver.
	 */
	Transaction refundTxReceiver;
	
	/**
	 * The refund tx receiver changed.
	 */
	boolean refundTxReceiverChanged;
	
	
	/**
	 * The add tx sender id.
	 */
	int addTxSenderID;
	
	/**
	 * The add tx sender.
	 */
	Transaction addTxSender;
	
	/**
	 * The add tx sender changed.
	 */
	boolean addTxSenderChanged;

	/**
	 * The add tx receiver id.
	 */
	int addTxReceiverID;
	
	/**
	 * The add tx receiver.
	 */
	Transaction addTxReceiver;
	
	/**
	 * The add tx receiver changed.
	 */
	boolean addTxReceiverChanged;
	
	/**
	 * The settlement tx sender temp id.
	 */
	int settlementTxSenderTempID;
	
	/**
	 * The settlement tx sender temp.
	 */
	Transaction settlementTxSenderTemp;
	
	/**
	 * The settlement tx sender temp changed.
	 */
	boolean settlementTxSenderTempChanged;
	
	/**
	 * The settlement tx receiver temp id.
	 */
	int settlementTxReceiverTempID;
	
	/**
	 * The settlement tx receiver temp.
	 */
	Transaction settlementTxReceiverTemp;
	
	/**
	 * The settlement tx receiver temp changed.
	 */
	boolean settlementTxReceiverTempChanged;


	/**
	 * The refund tx sender temp id.
	 */
	int refundTxSenderTempID;
	
	/**
	 * The refund tx sender temp.
	 */
	Transaction refundTxSenderTemp;
	
	/**
	 * The refund tx sender temp changed.
	 */
	boolean refundTxSenderTempChanged;

	/**
	 * The refund tx receiver temp id.
	 */
	int refundTxReceiverTempID;
	
	/**
	 * The refund tx receiver temp.
	 */
	Transaction refundTxReceiverTemp;
	
	/**
	 * The refund tx receiver temp changed.
	 */
	boolean refundTxReceiverTempChanged;
	
	
	/**
	 * The add tx sender temp id.
	 */
	int addTxSenderTempID;
	
	/**
	 * The add tx sender temp.
	 */
	Transaction addTxSenderTemp;
	
	/**
	 * The add tx sender temp changed.
	 */
	boolean addTxSenderTempChanged;

	/**
	 * The add tx receiver temp id.
	 */
	int addTxReceiverTempID;
	
	/**
	 * The add tx receiver temp.
	 */
	Transaction addTxReceiverTemp;
	
	/**
	 * The add tx receiver temp changed.
	 */
	boolean addTxReceiverTempChanged;


	/**
	 * Instantiates a new payment.
	 *
	 * @param result the result
	 * @throws SQLException the SQL exception
	 */
	public Payment(ResultSet result) throws SQLException {
		id = result.getInt("id");
		channelIdReceiver = result.getInt("channel_id_receiver");
		channelIdSender = result.getInt("channel_id_sender");
        amount = result.getLong("amount");
        fee = result.getLong("fee");
        phaseSender = result.getInt("phase_sender");
        phaseReceiver = result.getInt("phase_receiver");
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
	 * @param channelIdSender the channel id sender
	 * @param channelIdReceiver the channel id receiver
	 * @param amount the amount
	 * @param secretHash the secret hash
	 */
	public Payment(int channelIdSender, int channelIdReceiver, long amount, String secretHash) {
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
	 * @param receiver the receiver
	 * @param amount the amount
	 * @param secretHash the secret hash
	 */
	public Payment(int channelIdSender, String receiver, long amount, String secretHash) {
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
        this.phaseSender = 0;
        this.phaseReceiver = 0;
    }
	
	/**
	 * Instantiates a new payment.
	 *
	 * @param channelIdSender the channel id sender
	 * @param channelIdReceiver the channel id receiver
	 * @param amount the amount
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 */
	public Payment(int channelIdSender, int channelIdReceiver, long amount) throws UnsupportedEncodingException, NoSuchAlgorithmException {
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
	 * Replace current transactions with temporary.
	 */
	public void replaceCurrentTransactionsWithTemporary() {
        /**
         * Only update the transactions corresponding to that client..
         */
        if(paymentToServer) {
            int temp2 = settlementTxSenderID;
            int temp4 = refundTxSenderID;
            int temp6 = addTxSenderID;
            settlementTxSenderID = settlementTxSenderTempID;
            refundTxSenderID = refundTxSenderTempID;
            addTxSenderID = addTxSenderTempID;
            settlementTxSenderTempID = temp2;
            refundTxSenderTempID = temp4;
            addTxSenderTempID = temp6;
        } else {
            int temp1 = settlementTxReceiverID;
            int temp3 = refundTxReceiverID;
            int temp5 = addTxReceiverID;
            settlementTxReceiverID = settlementTxReceiverTempID;
            refundTxReceiverID = refundTxReceiverTempID;
            addTxReceiverID = addTxReceiverTempID;
            settlementTxReceiverTempID = temp1;
            refundTxReceiverTempID = temp3;
            addTxReceiverTempID = temp5;
        }

	}
	
	/**
	 * Update transactions to database.
	 *
	 * @param conn the conn
	 * @throws SQLException the SQL exception
	 */
	public void updateTransactionsToDatabase(Connection conn) throws SQLException {
        /**
         * Only update the transactions corresponding to that client..
         */
        if(paymentToServer) {
            if(settlementTxSenderChanged){
                settlementTxSenderID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(settlementTxSender, channelIdSender, settlementTxSenderID));
            }
            if(refundTxSenderChanged){
                refundTxSenderID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxSender, channelIdSender, refundTxSenderID));
            }
            if(addTxSenderChanged){
                addTxSenderID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(addTxSender, channelIdSender, addTxSenderID));
            }
            if(settlementTxSenderTempChanged){
                settlementTxSenderTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(settlementTxSenderTemp, channelIdSender, settlementTxSenderTempID));
            }
            if(refundTxSenderTempChanged){
                refundTxSenderTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxSenderTemp, channelIdSender, refundTxSenderTempID));
            }
            if(addTxSenderTempChanged){
                addTxSenderTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(addTxSenderTemp, channelIdSender, addTxSenderTempID));
            }
        } else {
            if(settlementTxReceiverChanged){
                settlementTxReceiverID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(settlementTxReceiver, channelIdReceiver, settlementTxReceiverID));
            }
            if(refundTxReceiverChanged){
                refundTxReceiverID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxReceiver, channelIdReceiver, refundTxReceiverID));
            }
            if(addTxReceiverChanged){
                addTxReceiverID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(addTxReceiver, channelIdReceiver, addTxReceiverID));
            }
            if(settlementTxReceiverTempChanged){
                settlementTxReceiverTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(settlementTxReceiverTemp, channelIdReceiver, settlementTxReceiverTempID));
            }
            if(refundTxReceiverTempChanged){
                refundTxReceiverTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxReceiverTemp, channelIdReceiver, refundTxReceiverTempID));
            }
            if(addTxReceiverTempChanged){
                addTxReceiverTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(addTxReceiverTemp, channelIdReceiver, addTxReceiverTempID));
            }
        }
	}
	
	
	/**
	 * Gets the settlement tx sender.
	 *
	 * @return the settlement tx sender
	 * @throws SQLException the SQL exception
	 */
	public Transaction getSettlementTxSender() throws SQLException {
		if(settlementTxSender == null)
			settlementTxSender = MySQLConnection.getTransaction(conn, settlementTxSenderID); 
		return settlementTxSender;
	}

	/**
	 * Sets the settlement tx sender.
	 *
	 * @param settlementTxSender the new settlement tx sender
	 */
	private void setSettlementTxSender(Transaction settlementTxSender) {
		settlementTxSenderChanged = true;
		this.settlementTxSender = settlementTxSender;
	}
	
	/**
	 * Gets the settlement tx sender id.
	 *
	 * @return the settlement tx sender id
	 */
	public int getSettlementTxSenderID() {
		return settlementTxSenderID;
	}
	
	/**
	 * Sets the settlement tx sender id.
	 *
	 * @param settlementTxSenderID the new settlement tx sender id
	 */
	public void setSettlementTxSenderID(int settlementTxSenderID) {
		this.settlementTxSenderID = settlementTxSenderID;
	}
	
	
	/**
	 * Gets the settlement tx receiver.
	 *
	 * @return the settlement tx receiver
	 * @throws SQLException the SQL exception
	 */
	public Transaction getSettlementTxReceiver() throws SQLException {
		if(settlementTxReceiver == null)
			settlementTxReceiver = MySQLConnection.getTransaction(conn, settlementTxReceiverID); 
		return settlementTxReceiver;
	}

	/**
	 * Sets the settlement tx receiver.
	 *
	 * @param settlementTxReceiver the new settlement tx receiver
	 */
	private void setSettlementTxReceiver(Transaction settlementTxReceiver) {
		settlementTxReceiverChanged = true;
		this.settlementTxReceiver = settlementTxReceiver;
	}
	
	/**
	 * Gets the settlement tx receiver id.
	 *
	 * @return the settlement tx receiver id
	 */
	public int getSettlementTxReceiverID() {
		return settlementTxReceiverID;
	}

	/**
	 * Sets the settlement tx receiver id.
	 *
	 * @param settlementTxReceiverID the new settlement tx receiver id
	 */
	public void setSettlementTxReceiverID(int settlementTxReceiverID) {
		this.settlementTxReceiverID = settlementTxReceiverID;
	}
	
	/**
	 * Gets the refund tx sender.
	 *
	 * @return the refund tx sender
	 * @throws SQLException the SQL exception
	 */
	public Transaction getRefundTxSender() throws SQLException {
		if(refundTxSender == null)
			refundTxSender = MySQLConnection.getTransaction(conn, refundTxSenderID); 
		return refundTxSender;
	}

	/**
	 * Sets the refund tx sender.
	 *
	 * @param refundTxSender the new refund tx sender
	 */
	private void setRefundTxSender(Transaction refundTxSender) {
		refundTxSenderChanged = true;
		this.refundTxSender = refundTxSender;
	}
	
	/**
	 * Gets the refund tx sender id.
	 *
	 * @return the refund tx sender id
	 */
	public int getRefundTxSenderID() {
		return refundTxSenderID;
	}
	
	/**
	 * Sets the refund tx sender id.
	 *
	 * @param refundTxSenderID the new refund tx sender id
	 */
	public void setRefundTxSenderID(int refundTxSenderID) {
		this.refundTxSenderID = refundTxSenderID;
	}
	
	
	/**
	 * Gets the refund tx receiver.
	 *
	 * @return the refund tx receiver
	 * @throws SQLException the SQL exception
	 */
	public Transaction getRefundTxReceiver() throws SQLException {
		if(refundTxReceiver == null)
			refundTxReceiver = MySQLConnection.getTransaction(conn, refundTxReceiverID); 
		return refundTxReceiver;
	}

	/**
	 * Sets the refund tx receiver.
	 *
	 * @param refundTxReceiver the new refund tx receiver
	 */
	private void setRefundTxReceiver(Transaction refundTxReceiver) {
		refundTxReceiverChanged = true;
		this.refundTxReceiver = refundTxReceiver;
	}
	
	/**
	 * Gets the refund tx receiver id.
	 *
	 * @return the refund tx receiver id
	 */
	public int getRefundTxReceiverID() {
		return refundTxReceiverID;
	}

	/**
	 * Sets the refund tx receiver id.
	 *
	 * @param refundTxReceiverID the new refund tx receiver id
	 */
	public void setRefundTxReceiverID(int refundTxReceiverID) {
		this.refundTxReceiverID = refundTxReceiverID;
	}

	/**
	 * Gets the conn.
	 *
	 * @return the conn
	 */
	public Connection getConn() {
		return conn;
	}

	/**
	 * Sets the conn.
	 *
	 * @param conn the new conn
	 */
	public void setConn(Connection conn) {
		this.conn = conn;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Gets the channel id sender.
	 *
	 * @return the channel id sender
	 */
	public int getChannelIdSender() {
		return channelIdSender;
	}

	/**
	 * Sets the channel id sender.
	 *
	 * @param channelIdSender the new channel id sender
	 */
	public void setChannelIdSender(int channelIdSender) {
		this.channelIdSender = channelIdSender;
	}

	/**
	 * Gets the channel id receiver.
	 *
	 * @return the channel id receiver
	 */
	public int getChannelIdReceiver() {
		return channelIdReceiver;
	}

	/**
	 * Sets the channel id receiver.
	 *
	 * @param channelIdReceiver the new channel id receiver
	 */
	public void setChannelIdReceiver(int channelIdReceiver) {
		this.channelIdReceiver = channelIdReceiver;
	}

	/**
	 * Gets the amount.
	 *
	 * @return the amount
	 */
	public long getAmount() {
		return amount;
	}

	/**
	 * Sets the amount.
	 *
	 * @param amount the new amount
	 */
	public void setAmount(long amount) {
		this.amount = amount;
	}



	/**
	 * Gets the secret hash.
	 *
	 * @return the secret hash
	 */
	public String getSecretHash() {
		return secretHash;
	}

	/**
	 * Sets the secret hash.
	 *
	 * @param secretHash the new secret hash
	 */
	public void setSecretHash(String secretHash) {
		this.secretHash = secretHash;
	}

	/**
	 * Gets the secret.
	 *
	 * @return the secret
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * Sets the secret.
	 *
	 * @param secret the new secret
	 */
	public void setSecret(String secret) {
		this.secret = secret;
	}

	/**
	 * Gets the timestamp created.
	 *
	 * @return the timestamp created
	 */
	public int getTimestampCreated() {
		return timestampCreated;
	}

	/**
	 * Sets the timestamp created.
	 *
	 * @param timestampCreated the new timestamp created
	 */
	public void setTimestampCreated(int timestampCreated) {
		this.timestampCreated = timestampCreated;
	}

    public int getTimestampSettledReceiver() {
        return timestampSettledReceiver;
    }

    public void setTimestampSettledReceiver(int timestampSettledReceiver) {
        this.timestampSettledReceiver = timestampSettledReceiver;
    }

    public int getTimestampSettledSender() {
        return timestampSettledSender;
    }

    public void setTimestampSettledSender(int timestampSettledSender) {
        this.timestampSettledSender = timestampSettledSender;
    }

    /**
	 * Checks if is include in sender channel.
	 *
	 * @return true, if is include in sender channel
	 */
	public boolean isIncludeInSenderChannel() {
		return includeInSenderChannel;
	}

	/**
	 * Sets the include in sender channel.
	 *
	 * @param includeInSenderChannel the new include in sender channel
	 */
	public void setIncludeInSenderChannel(boolean includeInSenderChannel) {
		this.includeInSenderChannel = includeInSenderChannel;
	}

	/**
	 * Checks if is include in receiver channel.
	 *
	 * @return true, if is include in receiver channel
	 */
	public boolean isIncludeInReceiverChannel() {
		return includeInReceiverChannel;
	}

	/**
	 * Sets the include in receiver channel.
	 *
	 * @param includeInReceiverChannel the new include in receiver channel
	 */
	public void setIncludeInReceiverChannel(boolean includeInReceiverChannel) {
		this.includeInReceiverChannel = includeInReceiverChannel;
	}
	
	/**
	 * Gets the adds the tx sender.
	 *
	 * @return the adds the tx sender
	 * @throws SQLException the SQL exception
	 */
	public Transaction getAddTxSender() throws SQLException {
		if(addTxSender == null)
			addTxSender = MySQLConnection.getTransaction(conn, addTxSenderID); 
		return addTxSender;
	}

	/**
	 * Sets the adds the tx sender.
	 *
	 * @param addTxSender the new adds the tx sender
	 */
	private void setAddTxSender(Transaction addTxSender) {
		addTxSenderChanged = true;
		this.addTxSender = addTxSender;
	}
	
	/**
	 * Gets the adds the tx sender id.
	 *
	 * @return the adds the tx sender id
	 */
	public int getAddTxSenderID() {
		return addTxSenderID;
	}
	
	/**
	 * Sets the adds the tx sender id.
	 *
	 * @param addTxSenderID the new adds the tx sender id
	 */
	public void setAddTxSenderID(int addTxSenderID) {
		this.addTxSenderID = addTxSenderID;
	}
	
	
	/**
	 * Gets the adds the tx receiver.
	 *
	 * @return the adds the tx receiver
	 * @throws SQLException the SQL exception
	 */
	public Transaction getAddTxReceiver() throws SQLException {
		if(addTxReceiver == null)
			addTxReceiver = MySQLConnection.getTransaction(conn, addTxReceiverID); 
		return addTxReceiver;
	}

	/**
	 * Sets the adds the tx receiver.
	 *
	 * @param addTxReceiver the new adds the tx receiver
	 */
	private void setAddTxReceiver(Transaction addTxReceiver) {
		addTxReceiverChanged = true;
		this.addTxReceiver = addTxReceiver;
	}
	
	/**
	 * Gets the adds the tx receiver id.
	 *
	 * @return the adds the tx receiver id
	 */
	public int getAddTxReceiverID() {
		return addTxReceiverID;
	}

	/**
	 * Sets the adds the tx receiver id.
	 *
	 * @param addTxReceiverID the new adds the tx receiver id
	 */
	public void setAddTxReceiverID(int addTxReceiverID) {
		this.addTxReceiverID = addTxReceiverID;
	}
	
	/**
	 * Gets the settlement tx sender temp.
	 *
	 * @return the settlement tx sender temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getSettlementTxSenderTemp() throws SQLException {
		if(settlementTxSenderTemp == null)
			settlementTxSenderTemp = MySQLConnection.getTransaction(conn, settlementTxSenderTempID); 
		return settlementTxSenderTemp;
	}

	/**
	 * Sets the settlement tx sender temp.
	 *
	 * @param settlementTxSenderTemp the new settlement tx sender temp
	 */
	public void setSettlementTxSenderTemp(Transaction settlementTxSenderTemp) {
		settlementTxSenderTempChanged = true;
		this.settlementTxSenderTemp = settlementTxSenderTemp;
	}
	
	/**
	 * Gets the settlement tx sender temp id.
	 *
	 * @return the settlement tx sender temp id
	 */
	public int getSettlementTxSenderTempID() {
		return settlementTxSenderTempID;
	}
	
	/**
	 * Sets the settlement tx sender temp id.
	 *
	 * @param settlementTxSenderTempID the new settlement tx sender temp id
	 */
	public void setSettlementTxSenderTempID(int settlementTxSenderTempID) {
		this.settlementTxSenderTempID = settlementTxSenderTempID;
	}
	
	
	/**
	 * Gets the settlement tx receiver temp.
	 *
	 * @return the settlement tx receiver temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getSettlementTxReceiverTemp() throws SQLException {
		if(settlementTxReceiverTemp == null)
			settlementTxReceiverTemp = MySQLConnection.getTransaction(conn, settlementTxReceiverTempID); 
		return settlementTxReceiverTemp;
	}

	/**
	 * Sets the settlement tx receiver temp.
	 *
	 * @param settlementTxReceiverTemp the new settlement tx receiver temp
	 */
	public void setSettlementTxReceiverTemp(Transaction settlementTxReceiverTemp) {
		settlementTxReceiverTempChanged = true;
		this.settlementTxReceiverTemp = settlementTxReceiverTemp;
	}
	
	/**
	 * Gets the settlement tx receiver temp id.
	 *
	 * @return the settlement tx receiver temp id
	 */
	public int getSettlementTxReceiverTempID() {
		return settlementTxReceiverTempID;
	}

	/**
	 * Sets the settlement tx receiver temp id.
	 *
	 * @param settlementTxReceiverTempID the new settlement tx receiver temp id
	 */
	public void setSettlementTxReceiverTempID(int settlementTxReceiverTempID) {
		this.settlementTxReceiverTempID = settlementTxReceiverTempID;
	}
	
	/**
	 * Gets the refund tx sender temp.
	 *
	 * @return the refund tx sender temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getRefundTxSenderTemp() throws SQLException {
		if(refundTxSenderTemp == null)
			refundTxSenderTemp = MySQLConnection.getTransaction(conn, refundTxSenderTempID); 
		return refundTxSenderTemp;
	}

	/**
	 * Sets the refund tx sender temp.
	 *
	 * @param refundTxSenderTemp the new refund tx sender temp
	 */
	public void setRefundTxSenderTemp(Transaction refundTxSenderTemp) {
		refundTxSenderTempChanged = true;
		this.refundTxSenderTemp = refundTxSenderTemp;
	}
	
	/**
	 * Gets the refund tx sender temp id.
	 *
	 * @return the refund tx sender temp id
	 */
	public int getRefundTxSenderTempID() {
		return refundTxSenderTempID;
	}
	
	/**
	 * Sets the refund tx sender temp id.
	 *
	 * @param refundTxSenderTempID the new refund tx sender temp id
	 */
	public void setRefundTxSenderTempID(int refundTxSenderTempID) {
		this.refundTxSenderTempID = refundTxSenderTempID;
	}
	
	
	/**
	 * Gets the refund tx receiver temp.
	 *
	 * @return the refund tx receiver temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getRefundTxReceiverTemp() throws SQLException {
		if(refundTxReceiverTemp == null)
			refundTxReceiverTemp = MySQLConnection.getTransaction(conn, refundTxReceiverTempID); 
		return refundTxReceiverTemp;
	}

	/**
	 * Sets the refund tx receiver temp.
	 *
	 * @param refundTxReceiverTemp the new refund tx receiver temp
	 */
	public void setRefundTxReceiverTemp(Transaction refundTxReceiverTemp) {
		refundTxReceiverTempChanged = true;
		this.refundTxReceiverTemp = refundTxReceiverTemp;
	}
	
	/**
	 * Gets the refund tx receiver temp id.
	 *
	 * @return the refund tx receiver temp id
	 */
	public int getRefundTxReceiverTempID() {
		return refundTxReceiverTempID;
	}

	/**
	 * Sets the refund tx receiver temp id.
	 *
	 * @param refundTxReceiverTempID the new refund tx receiver temp id
	 */
	public void setRefundTxReceiverTempID(int refundTxReceiverTempID) {
		this.refundTxReceiverTempID = refundTxReceiverTempID;
	}
	
	/**
	 * Gets the adds the tx sender temp.
	 *
	 * @return the adds the tx sender temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getAddTxSenderTemp() throws SQLException {
		if(addTxSenderTemp == null)
			addTxSenderTemp = MySQLConnection.getTransaction(conn, addTxSenderTempID); 
		return addTxSenderTemp;
	}

	/**
	 * Sets the adds the tx sender temp.
	 *
	 * @param addTxSenderTemp the new adds the tx sender temp
	 */
	public void setAddTxSenderTemp(Transaction addTxSenderTemp) {
		addTxSenderTempChanged = true;
		this.addTxSenderTemp = addTxSenderTemp;
	}
	
	/**
	 * Gets the adds the tx sender temp id.
	 *
	 * @return the adds the tx sender temp id
	 */
	public int getAddTxSenderTempID() {
		return addTxSenderTempID;
	}
	
	/**
	 * Sets the adds the tx sender temp id.
	 *
	 * @param addTxSenderTempID the new adds the tx sender temp id
	 */
	public void setAddTxSenderTempID(int addTxSenderTempID) {
		this.addTxSenderTempID = addTxSenderTempID;
	}
	
	
	/**
	 * Gets the adds the tx receiver temp.
	 *
	 * @return the adds the tx receiver temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getAddTxReceiverTemp() throws SQLException {
		if(addTxReceiverTemp == null)
			addTxReceiverTemp = MySQLConnection.getTransaction(conn, addTxReceiverTempID); 
		return addTxReceiverTemp;
	}

	/**
	 * Sets the adds the tx receiver temp.
	 *
	 * @param addTxReceiverTemp the new adds the tx receiver temp
	 */
	public void setAddTxReceiverTemp(Transaction addTxReceiverTemp) {
		addTxReceiverTempChanged = true;
		this.addTxReceiverTemp = addTxReceiverTemp;
	}
	
	/**
	 * Gets the adds the tx receiver temp id.
	 *
	 * @return the adds the tx receiver temp id
	 */
	public int getAddTxReceiverTempID() {
		return addTxReceiverTempID;
	}

	/**
	 * Sets the adds the tx receiver temp id.
	 *
	 * @param addTxReceiverTempID the new adds the tx receiver temp id
	 */
	public void setAddTxReceiverTempID(int addTxReceiverTempID) {
		this.addTxReceiverTempID = addTxReceiverTempID;
	}

	/**
	 * Gets the timestamp added to receiver.
	 *
	 * @return the timestamp added to receiver
	 */
	public int getTimestampAddedToReceiver() {
		return timestampAddedToReceiver;
	}

	/**
	 * Sets the timestamp added to receiver.
	 *
	 * @param timestampAddedToReceiver the new timestamp added to receiver
	 */
	public void setTimestampAddedToReceiver(int timestampAddedToReceiver) {
		this.timestampAddedToReceiver = timestampAddedToReceiver;
	}

	/**
	 * Gets the receiver.
	 *
	 * @return the receiver
	 */
	public String getReceiver() {
		return receiver;
	}

	/**
	 * Sets the receiver.
	 *
	 * @param receiver the new receiver
	 */
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

    public int getPhaseReceiver() {
        return phaseReceiver;
    }

    public void setPhaseReceiver(int phaseReceiver) {
        this.phaseReceiver = phaseReceiver;
    }

    public int getPhaseSender() {
        return phaseSender;
    }

    public void setPhaseSender(int phaseSender) {
        this.phaseSender = phaseSender;
    }

    public long getFee() {
        return fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    @Override
	public String toString() {
		return "Payment\n\tconn=" + conn + "\n\tid=" + id
				+ "\n\tchannelIdSender=" + channelIdSender
				+ "\n\tchannelIdReceiver=" + channelIdReceiver + "\n\tamount="
				+ amount + "\n\tphase=" + phaseReceiver + "\n\tphase=" + phaseSender + "\n\tsecretHash="
				+ secretHash + "\n\tsecret=" + secret + "\n\ttimestampCreated="
				+ timestampCreated
				+ "\n\ttimestampAddedToReceiver=" + timestampAddedToReceiver
				+ "\n\treceiver=" + receiver + "\n\tincludeInSenderChannel="
				+ includeInSenderChannel + "\n\tincludeInReceiverChannel="
				+ includeInReceiverChannel + "\n\tpaymentToServer="
				+ paymentToServer + "\n\tsettlementTxSenderID="
				+ settlementTxSenderID + "\n\tsettlementTxSender="
				+ settlementTxSender + "\n\tsettlementTxSenderChanged="
				+ settlementTxSenderChanged + "\n\tsettlementTxReceiverID="
				+ settlementTxReceiverID + "\n\tsettlementTxReceiver="
				+ settlementTxReceiver + "\n\tsettlementTxReceiverChanged="
				+ settlementTxReceiverChanged + "\n\trefundTxSenderID="
				+ refundTxSenderID + "\n\trefundTxSender=" + refundTxSender
				+ "\n\trefundTxSenderChanged=" + refundTxSenderChanged
				+ "\n\trefundTxReceiverID=" + refundTxReceiverID
				+ "\n\trefundTxReceiver=" + refundTxReceiver
				+ "\n\trefundTxReceiverChanged=" + refundTxReceiverChanged
				+ "\n\taddTxSenderID=" + addTxSenderID + "\n\taddTxSender="
				+ addTxSender + "\n\taddTxSenderChanged=" + addTxSenderChanged
				+ "\n\taddTxReceiverID=" + addTxReceiverID
				+ "\n\taddTxReceiver=" + addTxReceiver
				+ "\n\taddTxReceiverChanged=" + addTxReceiverChanged
				+ "\n\tsettlementTxSenderTempID=" + settlementTxSenderTempID
				+ "\n\tsettlementTxSenderTemp=" + settlementTxSenderTemp
				+ "\n\tsettlementTxSenderTempChanged="
				+ settlementTxSenderTempChanged
				+ "\n\tsettlementTxReceiverTempID="
				+ settlementTxReceiverTempID + "\n\tsettlementTxReceiverTemp="
				+ settlementTxReceiverTemp
				+ "\n\tsettlementTxReceiverTempChanged="
				+ settlementTxReceiverTempChanged + "\n\trefundTxSenderTempID="
				+ refundTxSenderTempID + "\n\trefundTxSenderTemp="
				+ refundTxSenderTemp + "\n\trefundTxSenderTempChanged="
				+ refundTxSenderTempChanged + "\n\trefundTxReceiverTempID="
				+ refundTxReceiverTempID + "\n\trefundTxReceiverTemp="
				+ refundTxReceiverTemp + "\n\trefundTxReceiverTempChanged="
				+ refundTxReceiverTempChanged + "\n\taddTxSenderTempID="
				+ addTxSenderTempID + "\n\taddTxSenderTemp=" + addTxSenderTemp
				+ "\n\taddTxSenderTempChanged=" + addTxSenderTempChanged
				+ "\n\taddTxReceiverTempID=" + addTxReceiverTempID
				+ "\n\taddTxReceiverTemp=" + addTxReceiverTemp
				+ "\n\taddTxReceiverTempChanged=" + addTxReceiverTempChanged;
	}

    public boolean isIncludeInReceiverChannelTemp() {
        return includeInReceiverChannelTemp;
    }

    public void setIncludeInReceiverChannelTemp(boolean includeInReceiverChannelTemp) {
        this.includeInReceiverChannelTemp = includeInReceiverChannelTemp;
    }

    public boolean isIncludeInSenderChannelTemp() {
        return includeInSenderChannelTemp;
    }

    public void setIncludeInSenderChannelTemp(boolean includeInSenderChannelTemp) {
        this.includeInSenderChannelTemp = includeInSenderChannelTemp;
    }

    public void setIncludedInChannelTemp(boolean includedInChannelTemp) {
        if(paymentToServer) {
            setIncludeInSenderChannelTemp(includedInChannelTemp);
        } else {
            setIncludeInReceiverChannelTemp(includedInChannelTemp);
        }
    }

    public void setIncludedInChannel(boolean includedInChannelTemp) {
        if(paymentToServer) {
            setIncludeInSenderChannel(includedInChannelTemp);
        } else {
            setIncludeInReceiverChannel(includedInChannelTemp);
        }
    }

    public void setPhase(int phase) {
        if(paymentToServer) {
            this.phaseSender = phase;
        } else {
            this.phaseReceiver = phase;
        }
    }

    public int getPhase() {
        if(paymentToServer) {
            return this.phaseSender;
        } else {
            return this.phaseReceiver;
        }
    }
}
