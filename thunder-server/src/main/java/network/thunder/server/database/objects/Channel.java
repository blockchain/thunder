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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import network.thunder.server.database.MySQLConnection;
import network.thunder.server.etc.Constants;
import network.thunder.server.etc.KeyDerivation;
import network.thunder.server.etc.SideConstants;
import network.thunder.server.etc.Tools;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;

// TODO: Auto-generated Javadoc

/**
 * TODO: We probably want very flexible rules for channels in the future. Currently, these rules are set as Constants in Constants.class.
 *          Add all those constants as columns to the Channels table.
 *          Seeing the channel table growing, it will become less and less clear.
 *          This change also has to be done on client side.
 *          Adding it here will mean that we have constant terms for the entirety of the channel, but with different terms for different channels.
 *          We can also - at a later point - include the possibility to change these terms with a protocol change.
 *
 * TODO: Add a 'Domain' field to the channels table (and consequently to this Class).
 *          This will allow the choice of the payment hub, and will also prepare for Multi-Hop solutions.
 *
 *          Other necessary work for Multi-Hop:
 *              - Have some system for routing payments in place
 *              - Have symmetric channels (requires a no-trust solution)
 *              - Have a INFO API, such that
 *                  - Users can see the terms, at which they will open a channel
 *                  - Other Payment Hubs can check, to which payment hubs this hub is connected to
 *
 * Communications for Payments from one Channel to another Channel will still follow the same rules we have in place currently.
 */

/**
 * The Class Channel.
 */
public class Channel {
	
	/**
	 * The conn.
	 */
	public Connection conn;
	
	/**
	 * The id.
	 */
	private int id;
	
	/**
	 * The pub key client.
	 */
	private String pubKeyClient;
	
	/**
	 * The pub key server.
	 */
	private String pubKeyServer;
	
	/**
	 * The change address server.
	 */
	private String changeAddressServer;
	
	/**
	 * The change address client.
	 */
	private String changeAddressClient;
	
	/**
	 * The master private key client.
	 */
	private String masterPrivateKeyClient;
	
	/**
	 * The master private key server.
	 */
	private String masterPrivateKeyServer;
	
	/**
	 * The initial amount server.
	 */
	private long initialAmountServer;
	
	/**
	 * The initial amount client.
	 */
	private long initialAmountClient;
	
	/**
	 * The amount server.
	 */
	private long amountServer;
	
	/**
	 * The amount client.
	 */
	private long amountClient;
	
	/**
	 * The timestamp open.
	 */
	private int timestampOpen;
	
	/**
	 * The timestamp close.
	 */
	private int timestampClose;
	
	/**
	 * The timestamp force close.
	 */
	private int timestampForceClose;
	
	/**
	 * The opening tx id.
	 */
	private int openingTxID;
	
	/**
	 * The refund tx server id.
	 */
	private int refundTxServerID;
	
	/**
	 * The refund tx client id.
	 */
	private int refundTxClientID;
	
	/**
	 * The channel tx server id.
	 */
	private int channelTxServerID;
	
	/**
	 * The channel tx client id.
	 */
	private int channelTxClientID;
	
	/**
	 * The channel tx server temp id.
	 */
	private int channelTxServerTempID;
	
	/**
	 * The channel tx client temp id.
	 */
	private int channelTxClientTempID;
	
	/**
	 * The channel tx revoke server id.
	 */
	private int channelTxRevokeServerID;
	
	/**
	 * The channel tx revoke client id.
	 */
	private int channelTxRevokeClientID;
	
	/**
	 * The channel tx revoke server temp id.
	 */
	private int channelTxRevokeServerTempID;
	
	/**
	 * The channel tx revoke client temp id.
	 */
	private int channelTxRevokeClientTempID;
	
	/**
	 * The has open payments.
	 */
	private boolean hasOpenPayments;
	
	/**
	 * The establish phase.
	 */
	private int establishPhase;
	
	/**
	 * The payment phase.
	 */
	private int paymentPhase;
	
	/**
	 * The is ready.
	 */
	private boolean isReady;
	
	/**
	 * The key chain depth.
	 */
	private int keyChainDepth;
	
	/**
	 * The key chain child.
	 */
	private int keyChainChild;
	
	/**
	 * The master chain depth.
	 */
	private int masterChainDepth;
	
	/**
	 * The opening tx hash.
	 */
	private String openingTxHash;
	
	/**
	 * The opening tx changed.
	 */
	private boolean openingTxChanged;
	
	/**
	 * The refund tx server changed.
	 */
	private boolean refundTxServerChanged;
	
	/**
	 * The refund tx client changed.
	 */
	private boolean refundTxClientChanged;
	
	/**
	 * The channel tx server changed.
	 */
	private boolean channelTxServerChanged;
	
	/**
	 * The channel tx client changed.
	 */
	private boolean channelTxClientChanged;
	
	/**
	 * The channel tx server temp changed.
	 */
	private boolean channelTxServerTempChanged;
	
	/**
	 * The channel tx client temp changed.
	 */
	private boolean channelTxClientTempChanged;
	
	/**
	 * The channel tx revoke server changed.
	 */
	private boolean channelTxRevokeServerChanged;
	
	/**
	 * The channel tx revoke client changed.
	 */
	private boolean channelTxRevokeClientChanged;
	
	/**
	 * The channel tx revoke server temp changed.
	 */
	private boolean channelTxRevokeServerTempChanged;
	
	/**
	 * The channel tx revoke client temp changed.
	 */
	private boolean channelTxRevokeClientTempChanged;
	
	/**
	 * The opening tx.
	 */
	private Transaction openingTx;
	
	/**
	 * The refund tx server.
	 */
	private Transaction refundTxServer;
	
	/**
	 * The refund tx client.
	 */
	private Transaction refundTxClient;
	
	/**
	 * The channel tx server.
	 */
	private Transaction channelTxServer;
	
	/**
	 * The channel tx client.
	 */
	private Transaction channelTxClient;
	
	/**
	 * The channel tx server temp.
	 */
	private Transaction channelTxServerTemp;
	
	/**
	 * The channel tx client temp.
	 */
	private Transaction channelTxClientTemp;
	
	/**
	 * The channel tx revoke server.
	 */
	private Transaction channelTxRevokeServer;
	
	/**
	 * The channel tx revoke client.
	 */
	private Transaction channelTxRevokeClient;
	
	/**
	 * The channel tx revoke server temp.
	 */
	private Transaction channelTxRevokeServerTemp;
	
	/**
	 * The channel tx revoke client temp.
	 */
	private Transaction channelTxRevokeClientTemp;
	
	/**
	 * The client key.
	 */
	private ECKey clientKey;
	
	/**
	 * The client key deterministic.
	 */
	private DeterministicKey clientKeyDeterministic;
	
	/**
	 * The server key.
	 */
	private ECKey serverKey;
	
	/**
	 * The server key deterministic.
	 */
	private DeterministicKey serverKeyDeterministic;
	
	/**
	 * Instantiates a new channel.
	 */
	public Channel() {}
	
	/**
	 * Instantiates a new channel.
	 *
	 * @param result the result
	 * @throws SQLException the SQL exception
	 */
	public Channel(ResultSet result) throws SQLException {
		this.setId(result.getInt("id"));
		this.setAmountServer(result.getLong("amount_server"));
		this.setAmountClient(result.getLong("amount_client"));
		this.setInitialAmountServer(result.getLong("initial_amount_server"));
		this.setInitialAmountClient(result.getLong("initial_amount_client"));

		this.setPubKeyClient(result.getString("pub_key_client"));
		this.setPubKeyServer(result.getString("pub_key_server"));
		this.setChangeAddressClient(result.getString("change_address_client"));
		this.setChangeAddressServer(result.getString("change_address_server"));
		this.setMasterPrivateKeyServer(result.getString("master_priv_key_server"));
		this.setMasterPrivateKeyClient(result.getString("master_priv_key_client"));
		
		this.setOpeningTxHash(result.getString("opening_tx_hash"));
		
		this.setTimestampClose(result.getInt("timestamp_close"));
		this.setTimestampOpen(result.getInt("timestamp_open"));
		this.setTimestampForceClose(result.getInt("timestamp_force_close"));
		this.setOpeningTxID(result.getInt("opening_tx"));
		this.setRefundTxServerID(result.getInt("refund_tx_server"));
		this.setRefundTxClientID(result.getInt("refund_tx_client"));
		this.setChannelTxServerID(result.getInt("channel_tx_server"));
		this.setChannelTxClientID(result.getInt("channel_tx_client"));
		this.setChannelTxServerTempID(result.getInt("channel_tx_server_temp"));
		this.setChannelTxClientTempID(result.getInt("channel_tx_client_temp"));
		this.setChannelTxRevokeServerID(result.getInt("channel_tx_revoke_server"));
		this.setChannelTxRevokeClientID(result.getInt("channel_tx_revoke_client"));
		this.setChannelTxRevokeServerTempID(result.getInt("channel_tx_revoke_server_temp"));
		this.setChannelTxRevokeClientTempID(result.getInt("channel_tx_revoke_client_temp"));
		
		this.setHasOpenPayments(Tools.intToBool(result.getInt("has_open_payments")));
		this.setReady(Tools.intToBool(result.getInt("is_ready")));
		this.setEstablishPhase(result.getInt("establish_phase"));
		this.setPaymentPhase(result.getInt("payment_phase"));
		
		this.setKeyChainChild(result.getInt("key_chain_child"));
		this.setKeyChainDepth(result.getInt("key_chain_depth"));
		this.setMasterChainDepth(result.getInt("master_chain_depth"));
	}
	
	/**
	 * Gets the transaction i ds.
	 *
	 * @return the transaction i ds
	 */
	public ArrayList<Integer> getTransactionIDs() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		if(openingTxID != 0) list.add(openingTxID);
		if(refundTxServerID != 0) list.add(refundTxServerID);
		if(refundTxClientID != 0) list.add(refundTxClientID);
		if(channelTxServerID != 0) list.add(channelTxServerID);
		if(channelTxClientID != 0) list.add(channelTxClientID);
		if(channelTxServerTempID != 0) list.add(channelTxServerTempID);
		if(channelTxClientTempID != 0) list.add(channelTxClientTempID);
		if(channelTxRevokeServerID != 0) list.add(channelTxRevokeServerID);
		if(channelTxRevokeClientID != 0) list.add(channelTxRevokeClientID);
		if(channelTxRevokeServerTempID != 0) list.add(channelTxRevokeServerTempID);
		if(channelTxRevokeClientTempID != 0) list.add(channelTxRevokeClientTempID);
		
		return list;
	}
	
	/**
	 * Replace current transactions with temporary.
	 *
	 * @throws SQLException the SQL exception
	 */
	public void replaceCurrentTransactionsWithTemporary() throws SQLException {
		
		int temp1 = channelTxClientID;
		int temp2 = channelTxServerID;
		int temp3 = channelTxRevokeClientID;
		int temp4 = channelTxRevokeServerID;
		
		channelTxClientID = channelTxClientTempID;
		channelTxServerID = channelTxServerTempID;
		channelTxRevokeClientID = channelTxRevokeClientTempID;
		channelTxRevokeServerID = channelTxRevokeServerTempID;
		
		channelTxClientTempID = temp1;
		channelTxServerTempID = temp2;
		channelTxRevokeClientTempID = temp3;
		channelTxRevokeServerTempID = temp4;
	}
	
	/**
	 * Gets the changed transactions.
	 *
	 * @return the changed transactions
	 */
	public ArrayList<Transaction> getChangedTransactions() {
		ArrayList<Transaction> list = new ArrayList<Transaction>();
		if(openingTxChanged) list.add(openingTx);
		if(refundTxServerChanged) list.add(refundTxServer);
		if(refundTxClientChanged) list.add(refundTxClient);
		if(channelTxServerChanged) list.add(channelTxServer);
		if(channelTxClientChanged) list.add(channelTxClient);
		if(channelTxServerTempChanged) list.add(channelTxServerTemp);
		if(channelTxClientTempChanged) list.add(channelTxClientTemp);
		if(channelTxRevokeServerChanged) list.add(channelTxRevokeServer);
		if(channelTxRevokeClientChanged) list.add(channelTxRevokeClient);
		if(channelTxRevokeServerTempChanged) list.add(channelTxRevokeServerTemp);
		if(channelTxRevokeClientTempChanged) list.add(channelTxRevokeClientTemp);
		
		return list;
	}
	
	/**
	 * Gets the change address client as address.
	 *
	 * @return the change address client as address
	 * @throws AddressFormatException the address format exception
	 */
	public Address getChangeAddressClientAsAddress() throws AddressFormatException {
		return new Address(Constants.getNetwork(), this.getChangeAddressClient());
	}
	
	/**
	 * Gets the change address server as address.
	 *
	 * @return the change address server as address
	 * @throws AddressFormatException the address format exception
	 */
	public Address getChangeAddressServerAsAddress() throws AddressFormatException {
		return new Address(Constants.getNetwork(), this.getChangeAddressServer());
	}
	
	/**
	 * Gets the timestamp refunds.
	 *
	 * @return the timestamp refunds
	 */
	public int getTimestampRefunds() {
		return this.timestampClose - Constants.SECURITY_TIME_WINDOW_BEFORE_CHANNEL_ENDS;
	}
	
	/**
	 * New master key.
	 *
	 * @param masterKey the master key
	 * @throws Exception the exception
	 */
	public void newMasterKey(Key masterKey) throws Exception {
		if( (SideConstants.RUNS_ON_SERVER && getMasterPrivateKeyClient() != null ) || (!SideConstants.RUNS_ON_SERVER && getMasterPrivateKeyServer() != null ) ) {
			/**
			 * Make sure the old masterPrivateKey is a child of this one..
			 */
			
			DeterministicKey key = DeterministicKey.deserializeB58(masterKey.privateKey, Constants.getNetwork());
			DeterministicHierarchy hierachy = new DeterministicHierarchy(key);
		
			List<ChildNumber> childList = KeyDerivation.getChildList(getMasterChainDepth() - masterKey.depth);
			DeterministicKey keyDerived = hierachy.get(childList, true, true);

			if(!KeyDerivation.compareDeterministicKeys(keyDerived, getMasterPrivateKeyClient()))
				throw new Exception("The new masterPrivateKey is not a parent of the one we have..");
		}
		
		if(SideConstants.RUNS_ON_SERVER) {
			setMasterPrivateKeyClient(masterKey.privateKey);
		} else {
			setMasterPrivateKeyServer(masterKey.privateKey);
		}
		setMasterChainDepth(masterKey.depth);
	}
	
	/**
	 * Gets the hierachy client.
	 *
	 * @return the hierachy client
	 */
	public DeterministicHierarchy getHierachyClient() {
		DeterministicKey masterKey = DeterministicKey.deserializeB58(getMasterPrivateKeyClient(), Constants.getNetwork());
		return new DeterministicHierarchy(masterKey);
	}
	
	/**
	 * Gets the hierachy server.
	 *
	 * @return the hierachy server
	 */
	public DeterministicHierarchy getHierachyServer() {
		DeterministicKey masterKey = DeterministicKey.deserializeB58(getMasterPrivateKeyServer(), Constants.getNetwork());
		return new DeterministicHierarchy(masterKey);
	}

	
	
	
	/**
	 * Update transactions to database.
	 *
	 * @param conn the conn
	 * @throws SQLException the SQL exception
	 */
	public void updateTransactionsToDatabase(Connection conn) throws SQLException {
		if(openingTxChanged){
			openingTxID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(openingTx, id, openingTxID));
		}
		if(refundTxServerChanged){
			refundTxServerID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxServer, id, refundTxServerID));
		}
		if(refundTxClientChanged){
			refundTxClientID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxClient, id, refundTxClientID));
		}
		if(channelTxServerChanged){
			channelTxServerID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxServer, id, channelTxServerID));
		}
		if(channelTxClientChanged){
			channelTxClientID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxClient, id, channelTxClientID));
		}
		if(channelTxServerTempChanged){
			channelTxServerTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxServerTemp, id, channelTxServerTempID));
		}
		if(channelTxClientTempChanged){
			channelTxClientTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxClientTemp, id, channelTxClientTempID));
		}
		if(channelTxRevokeServerChanged){
			channelTxRevokeServerID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxRevokeServer, id, channelTxRevokeServerID));
		}
		if(channelTxRevokeClientChanged){
			channelTxRevokeClientID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxRevokeClient, id, channelTxRevokeClientID));
		}
		if(channelTxRevokeServerTempChanged){
			channelTxRevokeServerTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxRevokeServerTemp, id, channelTxRevokeServerTempID));
		}
		if(channelTxRevokeClientTempChanged){
			channelTxRevokeClientTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxRevokeClientTemp, id, channelTxRevokeClientTempID));
		}
		
	}
	
	
	/**
	 * Gets the opening tx.
	 *
	 * @return the opening tx
	 * @throws SQLException the SQL exception
	 */
	public Transaction getOpeningTx() throws SQLException {
		if(openingTx == null)
			openingTx = MySQLConnection.getTransaction(conn, openingTxID);
		return openingTx;
	}

	/**
	 * Sets the opening tx.
	 *
	 * @param openingTx the new opening tx
	 */
	public void setOpeningTx(Transaction openingTx) {
		openingTxChanged = true;
		this.openingTx = openingTx;
	}

	/**
	 * Gets the refund tx server.
	 *
	 * @return the refund tx server
	 * @throws SQLException the SQL exception
	 */
	public Transaction getRefundTxServer() throws SQLException {
		if(refundTxServer == null)
			refundTxServer = MySQLConnection.getTransaction(conn, refundTxServerID);
		return refundTxServer;
	}

	/**
	 * Sets the refund tx server.
	 *
	 * @param refundTxMe the new refund tx server
	 */
	public void setRefundTxServer(Transaction refundTxMe) {
		refundTxServerChanged = true;
		this.refundTxServer = refundTxMe;
	}

	/**
	 * Gets the refund tx client.
	 *
	 * @return the refund tx client
	 * @throws SQLException the SQL exception
	 */
	public Transaction getRefundTxClient() throws SQLException {
		if(refundTxClient == null)
			refundTxClient = MySQLConnection.getTransaction(conn, refundTxClientID); 
		return refundTxClient;
	}

	/**
	 * Sets the refund tx client.
	 *
	 * @param refundTxYou the new refund tx client
	 */
	public void setRefundTxClient(Transaction refundTxYou) {
		refundTxClientChanged = true;
		this.refundTxClient = refundTxYou;
	}

	/**
	 * Gets the channel tx server.
	 *
	 * @return the channel tx server
	 * @throws SQLException the SQL exception
	 */
	public Transaction getChannelTxServer() throws SQLException {
		if(channelTxServer == null)
			channelTxServer = MySQLConnection.getTransaction(conn, channelTxServerID); 
		return channelTxServer;
	}

	/**
	 * Sets the channel tx server.
	 *
	 * @param channelTxMe the new channel tx server
	 */
	public void setChannelTxServer(Transaction channelTxMe) {
		channelTxServerChanged = true;
		this.channelTxServer = channelTxMe;
	}

	/**
	 * Gets the channel tx client.
	 *
	 * @return the channel tx client
	 * @throws SQLException the SQL exception
	 */
	public Transaction getChannelTxClient() throws SQLException {
		if(channelTxClient == null)
			channelTxClient = MySQLConnection.getTransaction(conn, channelTxClientID); 
		return channelTxClient;
	}

	/**
	 * Sets the channel tx client.
	 *
	 * @param channelTxYou the new channel tx client
	 */
	public void setChannelTxClient(Transaction channelTxYou) {
		channelTxClientChanged = true;
		this.channelTxClient = channelTxYou;
	}

	/**
	 * Gets the channel tx server temp.
	 *
	 * @return the channel tx server temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getChannelTxServerTemp() throws SQLException {
		if(channelTxServerTemp == null)
			channelTxServerTemp = MySQLConnection.getTransaction(conn, channelTxServerTempID); 
		return channelTxServerTemp;
	}

	/**
	 * Sets the channel tx server temp.
	 *
	 * @param channelTxMeTemp the new channel tx server temp
	 */
	public void setChannelTxServerTemp(Transaction channelTxMeTemp) {
		channelTxServerTempChanged = true;
		this.channelTxServerTemp = channelTxMeTemp;
	}

	/**
	 * Gets the channel tx client temp.
	 *
	 * @return the channel tx client temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getChannelTxClientTemp() throws SQLException {
		if(channelTxClientTemp == null)
			channelTxClientTemp = MySQLConnection.getTransaction(conn, channelTxClientTempID); 
		return channelTxClientTemp;
	}

	/**
	 * Sets the channel tx client temp.
	 *
	 * @param channelTxYouTemp the new channel tx client temp
	 */
	public void setChannelTxClientTemp(Transaction channelTxYouTemp) {
		channelTxClientTempChanged = true;
		this.channelTxClientTemp = channelTxYouTemp;
	}

	/**
	 * Gets the channel tx revoke server.
	 *
	 * @return the channel tx revoke server
	 * @throws SQLException the SQL exception
	 */
	public Transaction getChannelTxRevokeServer() throws SQLException {
		if(channelTxRevokeServer == null)
			channelTxRevokeServer = MySQLConnection.getTransaction(conn, channelTxRevokeServerID); 
		return channelTxRevokeServer;
	}

	/**
	 * Sets the channel tx revoke server.
	 *
	 * @param channelTxRevokeMe the new channel tx revoke server
	 */
	public void setChannelTxRevokeServer(Transaction channelTxRevokeMe) {
		channelTxRevokeServerChanged = true;
		this.channelTxRevokeServer = channelTxRevokeMe;
	}

	/**
	 * Gets the channel tx revoke client.
	 *
	 * @return the channel tx revoke client
	 * @throws SQLException the SQL exception
	 */
	public Transaction getChannelTxRevokeClient() throws SQLException {
		if(channelTxRevokeClient == null)
			channelTxRevokeClient = MySQLConnection.getTransaction(conn, channelTxRevokeClientID); 
		return channelTxRevokeClient;
	}

	/**
	 * Sets the channel tx revoke client.
	 *
	 * @param channelTxRevokeYou the new channel tx revoke client
	 */
	public void setChannelTxRevokeClient(Transaction channelTxRevokeYou) {
		channelTxRevokeClientChanged = true;
		this.channelTxRevokeClient = channelTxRevokeYou;
	}

	/**
	 * Gets the channel tx revoke server temp.
	 *
	 * @return the channel tx revoke server temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getChannelTxRevokeServerTemp() throws SQLException {
		if(channelTxRevokeServerTemp == null)
			channelTxRevokeServerTemp = MySQLConnection.getTransaction(conn, channelTxRevokeServerTempID); 
		return channelTxRevokeServerTemp;
	}

	/**
	 * Sets the channel tx revoke server temp.
	 *
	 * @param channelTxRevokeMeTemp the new channel tx revoke server temp
	 */
	public void setChannelTxRevokeServerTemp(Transaction channelTxRevokeMeTemp) {
		channelTxRevokeServerTempChanged = true;
		this.channelTxRevokeServerTemp = channelTxRevokeMeTemp;
	}

	/**
	 * Gets the channel tx revoke client temp.
	 *
	 * @return the channel tx revoke client temp
	 * @throws SQLException the SQL exception
	 */
	public Transaction getChannelTxRevokeClientTemp() throws SQLException {
		if(channelTxRevokeClientTemp == null)
			channelTxRevokeClientTemp = MySQLConnection.getTransaction(conn, channelTxRevokeClientTempID); 
		return channelTxRevokeClientTemp;
	}

	/**
	 * Sets the channel tx revoke client temp.
	 *
	 * @param channelTxRevokeYouTemp the new channel tx revoke client temp
	 */
	public void setChannelTxRevokeClientTemp(Transaction channelTxRevokeYouTemp) {
		channelTxRevokeClientTempChanged = true;
		this.channelTxRevokeClientTemp = channelTxRevokeYouTemp;
	}
	
	
	/**
	 * Gets the client key on server.
	 *
	 * @return the client key on server
	 */
	public ECKey getClientKeyOnServer() {
		if(clientKey == null) {
			clientKey = ECKey.fromPublicOnly(Tools.stringToByte(pubKeyClient));
		}
		return clientKey;
	}
	
	/**
	 * Gets the client key on client.
	 *
	 * @return the client key on client
	 */
	public ECKey getClientKeyOnClient() {
		if(clientKey == null) {
			clientKey = getClientKeyDeterministic();
		}
		return clientKey;
	}
	
	/**
	 * Gets the server key on server.
	 *
	 * @return the server key on server
	 */
	public ECKey getServerKeyOnServer() {
		if(serverKey == null) {
//			serverKey = DeterministicKey.deserializeB58(masterPrivateKeyMe, Constants.getNetwork());
			serverKey = getServerKeyDeterministic();
		}
		return serverKey;
	}
	

	/**
	 * Gets the server key on client.
	 *
	 * @return the server key on client
	 */
	public ECKey getServerKeyOnClient() {
		if(serverKey == null) {
//			serverKey = DeterministicKey.deserializeB58(masterPrivateKeyMe, Constants.getNetwork());
			serverKey = ECKey.fromPublicOnly(Tools.stringToByte(pubKeyServer));
		}
		return serverKey;
	}
	
	/**
	 * Gets the server key deterministic.
	 *
	 * @return the server key deterministic
	 */
	public DeterministicKey getServerKeyDeterministic() {
		if(serverKeyDeterministic == null) {
			serverKeyDeterministic = DeterministicKey.deserializeB58(this.getMasterPrivateKeyServer(), Constants.getNetwork());
//			serverKeyDeterministic = DeterministicKey.deserializeB58(null, masterPrivateKeyServer);
		}
		return serverKeyDeterministic;
	}
	
	/**
	 * Gets the client key deterministic.
	 *
	 * @return the client key deterministic
	 */
	public DeterministicKey getClientKeyDeterministic() {
		if(clientKeyDeterministic == null) {
			clientKeyDeterministic = DeterministicKey.deserializeB58(this.getMasterPrivateKeyClient(), Constants.getNetwork());
//			clientKeyDeterministic = DeterministicKey.deserializeB58(null, masterPrivateKeyClient);
		}
		return clientKeyDeterministic;
	}
	
	

	/**
	 * Gets the pub key client.
	 *
	 * @return the pub key client
	 */
	public String getPubKeyClient() {
		return pubKeyClient;
	}
	
	
	/**
	 * Sets the pub key client.
	 *
	 * @param pubKey the new pub key client
	 */
	public void setPubKeyClient(String pubKey) {
		this.pubKeyClient = pubKey;
	}

	/**
	 * Gets the change address server.
	 *
	 * @return the change address server
	 */
	public String getChangeAddressServer() {
		return changeAddressServer;
	}

	/**
	 * Sets the change address server.
	 *
	 * @param changeAddressServer the new change address server
	 */
	public void setChangeAddressServer(String changeAddressServer) {
		this.changeAddressServer = changeAddressServer;
	}

	/**
	 * Gets the change address client.
	 *
	 * @return the change address client
	 */
	public String getChangeAddressClient() {
		return changeAddressClient;
	}

	/**
	 * Sets the change address client.
	 *
	 * @param changeAddressClient the new change address client
	 */
	public void setChangeAddressClient(String changeAddressClient) {
		this.changeAddressClient = changeAddressClient;
	}

	/**
	 * Gets the master private key client.
	 *
	 * @return the master private key client
	 */
	public String getMasterPrivateKeyClient() {
		return masterPrivateKeyClient;
	}
	
	/**
	 * Sets the master private key client.
	 *
	 * @param masterPrivateKeyYou the new master private key client
	 */
	public void setMasterPrivateKeyClient(String masterPrivateKeyYou) {
		this.masterPrivateKeyClient = masterPrivateKeyYou;
	}
	
	/**
	 * Gets the master private key server.
	 *
	 * @return the master private key server
	 */
	public String getMasterPrivateKeyServer() {
		return masterPrivateKeyServer;
	}
	
	/**
	 * Sets the master private key server.
	 *
	 * @param masterPrivateKeyMe the new master private key server
	 */
	public void setMasterPrivateKeyServer(String masterPrivateKeyMe) {
		this.masterPrivateKeyServer = masterPrivateKeyMe;
	}
	
	/**
	 * Gets the initial amount server.
	 *
	 * @return the initial amount server
	 */
	public long getInitialAmountServer() {
		return initialAmountServer;
	}
	
	/**
	 * Sets the initial amount server.
	 *
	 * @param initialAmountMe the new initial amount server
	 */
	public void setInitialAmountServer(long initialAmountMe) {
		this.initialAmountServer = initialAmountMe;
	}
	
	/**
	 * Gets the initial amount client.
	 *
	 * @return the initial amount client
	 */
	public long getInitialAmountClient() {
		return initialAmountClient;
	}
	
	/**
	 * Sets the initial amount client.
	 *
	 * @param initialAmountYou the new initial amount client
	 */
	public void setInitialAmountClient(long initialAmountYou) {
		this.initialAmountClient = initialAmountYou;
	}
	
	/**
	 * Gets the amount server.
	 *
	 * @return the amount server
	 */
	public long getAmountServer() {
		return amountServer;
	}
	
	/**
	 * Sets the amount server.
	 *
	 * @param amountMe the new amount server
	 */
	public void setAmountServer(long amountMe) {
		this.amountServer = amountMe;
	}
	
	/**
	 * Gets the amount client.
	 *
	 * @return the amount client
	 */
	public long getAmountClient() {
		return amountClient;
	}
	
	/**
	 * Sets the amount client.
	 *
	 * @param amountYou the new amount client
	 */
	public void setAmountClient(long amountYou) {
		this.amountClient = amountYou;
	}
	
	/**
	 * Gets the timestamp open.
	 *
	 * @return the timestamp open
	 */
	public int getTimestampOpen() {
		return timestampOpen;
	}
	
	/**
	 * Sets the timestamp open.
	 *
	 * @param timestampOpen the new timestamp open
	 */
	public void setTimestampOpen(int timestampOpen) {
		this.timestampOpen = timestampOpen;
	}
	
	/**
	 * Gets the timestamp close.
	 *
	 * @return the timestamp close
	 */
	public int getTimestampClose() {
		return timestampClose;
	}
	
	/**
	 * Sets the timestamp close.
	 *
	 * @param timestampClose the new timestamp close
	 */
	public void setTimestampClose(int timestampClose) {
		this.timestampClose = timestampClose;
	}
	
	/**
	 * Gets the opening tx id.
	 *
	 * @return the opening tx id
	 */
	public int getOpeningTxID() {
		return openingTxID;
	}
	
	/**
	 * Sets the opening tx id.
	 *
	 * @param openingTxID the new opening tx id
	 */
	public void setOpeningTxID(int openingTxID) {
		this.openingTxID = openingTxID;
	}
	
	/**
	 * Gets the refund tx server id.
	 *
	 * @return the refund tx server id
	 */
	public int getRefundTxServerID() {
		return refundTxServerID;
	}
	
	/**
	 * Sets the refund tx server id.
	 *
	 * @param refundTxMeID the new refund tx server id
	 */
	public void setRefundTxServerID(int refundTxMeID) {
		this.refundTxServerID = refundTxMeID;
	}
	
	/**
	 * Gets the refund tx client id.
	 *
	 * @return the refund tx client id
	 */
	public int getRefundTxClientID() {
		return refundTxClientID;
	}
	
	/**
	 * Sets the refund tx client id.
	 *
	 * @param refundTxYouID the new refund tx client id
	 */
	public void setRefundTxClientID(int refundTxYouID) {
		this.refundTxClientID = refundTxYouID;
	}
	
	/**
	 * Gets the channel tx server id.
	 *
	 * @return the channel tx server id
	 */
	public int getChannelTxServerID() {
		return channelTxServerID;
	}
	
	/**
	 * Sets the channel tx server id.
	 *
	 * @param channelTxMeID the new channel tx server id
	 */
	public void setChannelTxServerID(int channelTxMeID) {
		this.channelTxServerID = channelTxMeID;
	}
	
	/**
	 * Gets the channel tx client id.
	 *
	 * @return the channel tx client id
	 */
	public int getChannelTxClientID() {
		return channelTxClientID;
	}
	
	/**
	 * Sets the channel tx client id.
	 *
	 * @param channelTxYouID the new channel tx client id
	 */
	public void setChannelTxClientID(int channelTxYouID) {
		this.channelTxClientID = channelTxYouID;
	}
	
	/**
	 * Gets the channel tx server temp id.
	 *
	 * @return the channel tx server temp id
	 */
	public int getChannelTxServerTempID() {
		return channelTxServerTempID;
	}
	
	/**
	 * Sets the channel tx server temp id.
	 *
	 * @param channelTxMeTempID the new channel tx server temp id
	 */
	public void setChannelTxServerTempID(int channelTxMeTempID) {
		this.channelTxServerTempID = channelTxMeTempID;
	}
	
	/**
	 * Gets the channel tx client temp id.
	 *
	 * @return the channel tx client temp id
	 */
	public int getChannelTxClientTempID() {
		return channelTxClientTempID;
	}
	
	/**
	 * Sets the channel tx client temp id.
	 *
	 * @param channelTxYouTempID the new channel tx client temp id
	 */
	public void setChannelTxClientTempID(int channelTxYouTempID) {
		this.channelTxClientTempID = channelTxYouTempID;
	}
	
	/**
	 * Gets the channel tx revoke server id.
	 *
	 * @return the channel tx revoke server id
	 */
	public int getChannelTxRevokeServerID() {
		return channelTxRevokeServerID;
	}
	
	/**
	 * Sets the channel tx revoke server id.
	 *
	 * @param channelTxRevokeMeID the new channel tx revoke server id
	 */
	public void setChannelTxRevokeServerID(int channelTxRevokeMeID) {
		this.channelTxRevokeServerID = channelTxRevokeMeID;
	}
	
	/**
	 * Gets the channel tx revoke client id.
	 *
	 * @return the channel tx revoke client id
	 */
	public int getChannelTxRevokeClientID() {
		return channelTxRevokeClientID;
	}
	
	/**
	 * Sets the channel tx revoke client id.
	 *
	 * @param channelTxRevokeYouID the new channel tx revoke client id
	 */
	public void setChannelTxRevokeClientID(int channelTxRevokeYouID) {
		this.channelTxRevokeClientID = channelTxRevokeYouID;
	}
	
	/**
	 * Gets the checks for open payments.
	 *
	 * @return the checks for open payments
	 */
	public boolean getHasOpenPayments() {
		return hasOpenPayments;
	}
	
	/**
	 * Sets the checks for open payments.
	 *
	 * @param hasOpenPayments the new checks for open payments
	 */
	public void setHasOpenPayments(boolean hasOpenPayments) {
		this.hasOpenPayments = hasOpenPayments;
	}
	
	/**
	 * Checks if is ready.
	 *
	 * @return true, if is ready
	 */
	public boolean isReady() {
		return isReady;
	}
	
	/**
	 * Sets the ready.
	 *
	 * @param isReady the new ready
	 */
	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}
	
	/**
	 * Gets the establish phase.
	 *
	 * @return the establish phase
	 */
	public int getEstablishPhase() {
		return establishPhase;
	}

	/**
	 * Sets the establish phase.
	 *
	 * @param establishPhase the new establish phase
	 */
	public void setEstablishPhase(int establishPhase) {
		this.establishPhase = establishPhase;
	}
	
	/**
	 * Gets the pub key server.
	 *
	 * @return the pub key server
	 */
	public String getPubKeyServer() {
		return pubKeyServer;
	}

	/**
	 * Sets the pub key server.
	 *
	 * @param pubKeyServer the new pub key server
	 */
	public void setPubKeyServer(String pubKeyServer) {
		this.pubKeyServer = pubKeyServer;
	}
	
	/**
	 * Gets the key chain depth.
	 *
	 * @return the key chain depth
	 */
	public int getKeyChainDepth() {
		return keyChainDepth;
	}
	
	/**
	 * Sets the key chain depth.
	 *
	 * @param keyChainDepth the new key chain depth
	 */
	public void setKeyChainDepth(int keyChainDepth) {
		this.keyChainDepth = keyChainDepth;
	}
	
	/**
	 * Gets the key chain child.
	 *
	 * @return the key chain child
	 */
	public int getKeyChainChild() {
		return keyChainChild;
	}
	
	/**
	 * Sets the key chain child.
	 *
	 * @param keyChainChild the new key chain child
	 */
	public void setKeyChainChild(int keyChainChild) {
		this.keyChainChild = keyChainChild;
	}
	
	/**
	 * Gets the payment phase.
	 *
	 * @return the payment phase
	 */
	public int getPaymentPhase() {
		return paymentPhase;
	}
	
	/**
	 * Sets the payment phase.
	 *
	 * @param paymentPhase the new payment phase
	 */
	public void setPaymentPhase(int paymentPhase) {
		this.paymentPhase = paymentPhase;
	}
	
	/**
	 * Gets the channel tx revoke server temp id.
	 *
	 * @return the channel tx revoke server temp id
	 */
	public int getChannelTxRevokeServerTempID() {
		return channelTxRevokeServerTempID;
	}
	
	/**
	 * Sets the channel tx revoke server temp id.
	 *
	 * @param channelTxRevokeServerTempID the new channel tx revoke server temp id
	 */
	public void setChannelTxRevokeServerTempID(int channelTxRevokeServerTempID) {
		this.channelTxRevokeServerTempID = channelTxRevokeServerTempID;
	}
	
	/**
	 * Gets the channel tx revoke client temp id.
	 *
	 * @return the channel tx revoke client temp id
	 */
	public int getChannelTxRevokeClientTempID() {
		return channelTxRevokeClientTempID;
	}
	
	/**
	 * Sets the channel tx revoke client temp id.
	 *
	 * @param channelTxRevokeClientTempID the new channel tx revoke client temp id
	 */
	public void setChannelTxRevokeClientTempID(int channelTxRevokeClientTempID) {
		this.channelTxRevokeClientTempID = channelTxRevokeClientTempID;
	}
	
	/**
	 * Gets the master chain depth.
	 *
	 * @return the master chain depth
	 */
	public int getMasterChainDepth() {
		return masterChainDepth;
	}
	
	/**
	 * Sets the master chain depth.
	 *
	 * @param masterChainDepth the new master chain depth
	 */
	public void setMasterChainDepth(int masterChainDepth) {
		this.masterChainDepth = masterChainDepth;
	}
	
	/**
	 * Gets the opening tx hash.
	 *
	 * @return the opening tx hash
	 */
	public String getOpeningTxHash() {
		return openingTxHash;
	}
	
	/**
	 * Sets the opening tx hash.
	 *
	 * @param openingTxHash the new opening tx hash
	 */
	public void setOpeningTxHash(String openingTxHash) {
		this.openingTxHash = openingTxHash;
	}
	
	/**
	 * Gets the timestamp force close.
	 *
	 * @return the timestamp force close
	 */
	public int getTimestampForceClose() {
		return timestampForceClose;
	}
	
	/**
	 * Sets the timestamp force close.
	 *
	 * @param timestampForceClose the new timestamp force close
	 */
	public void setTimestampForceClose(int timestampForceClose) {
		this.timestampForceClose = timestampForceClose;
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

	@Override
	public String toString() {
		try {
			return "Channel\n\tconn=" + conn + "\n\tid=" + id + "\n\tpubKeyClient="
					+ pubKeyClient + "\n\tpubKeyServer=" + pubKeyServer
					+ "\n\tchangeAddressServer=" + changeAddressServer
					+ "\n\tchangeAddressClient=" + changeAddressClient
					+ "\n\tmasterPrivateKeyClient=" + masterPrivateKeyClient
					+ "\n\tmasterPrivateKeyServer=" + masterPrivateKeyServer
					+ "\n\tinitialAmountServer=" + initialAmountServer
					+ "\n\tinitialAmountClient=" + initialAmountClient
					+ "\n\tamountServer=" + amountServer + "\n\tamountClient="
					+ amountClient + "\n\ttimestampOpen=" + timestampOpen
					+ "\n\ttimestampClose=" + timestampClose
					+ "\n\ttimestampForceClose=" + timestampForceClose
					+ "\n\topeningTxID=" + openingTxID + "\n\trefundTxServerID="
					+ refundTxServerID + "\n\trefundTxClientID=" + refundTxClientID
					+ "\n\tchannelTxServerID=" + channelTxServerID
					+ "\n\tchannelTxClientID=" + channelTxClientID
					+ "\n\tchannelTxServerTempID=" + channelTxServerTempID
					+ "\n\tchannelTxClientTempID=" + channelTxClientTempID
					+ "\n\tchannelTxRevokeServerID=" + channelTxRevokeServerID
					+ "\n\tchannelTxRevokeClientID=" + channelTxRevokeClientID
					+ "\n\tchannelTxRevokeServerTempID="
					+ channelTxRevokeServerTempID
					+ "\n\tchannelTxRevokeClientTempID="
					+ channelTxRevokeClientTempID + "\n\thasOpenPayments="
					+ hasOpenPayments + "\n\testablishPhase=" + establishPhase
					+ "\n\tpaymentPhase=" + paymentPhase + "\n\tisReady=" + isReady
					+ "\n\tkeyChainDepth=" + keyChainDepth + "\n\tkeyChainChild="
					+ keyChainChild + "\n\tmasterChainDepth=" + masterChainDepth
					+ "\n\topeningTxHash=" + openingTxHash
					+ "\n\topeningTxChanged=" + openingTxChanged
					+ "\n\trefundTxServerChanged=" + refundTxServerChanged
					+ "\n\trefundTxClientChanged=" + refundTxClientChanged
					+ "\n\tchannelTxServerChanged=" + channelTxServerChanged
					+ "\n\tchannelTxClientChanged=" + channelTxClientChanged
					+ "\n\tchannelTxServerTempChanged="
					+ channelTxServerTempChanged
					+ "\n\tchannelTxClientTempChanged="
					+ channelTxClientTempChanged
					+ "\n\tchannelTxRevokeServerChanged="
					+ channelTxRevokeServerChanged
					+ "\n\tchannelTxRevokeClientChanged="
					+ channelTxRevokeClientChanged
					+ "\n\tchannelTxRevokeServerTempChanged="
					+ channelTxRevokeServerTempChanged
					+ "\n\tchannelTxRevokeClientTempChanged="
					+ channelTxRevokeClientTempChanged + "\n\topeningTx="
					+ openingTx + "\n\trefundTxServer=" + refundTxServer
					+ "\n\trefundTxClient=" + refundTxClient
					+ "\n\tchannelTxServer=" + channelTxServer
					+ "\n\tchannelTxClient=" + channelTxClient
					+ "\n\tchannelTxServerTemp=" + channelTxServerTemp
					+ "\n\tchannelTxClientTemp=" + channelTxClientTemp
					+ "\n\tchannelTxRevokeServer=" + channelTxRevokeServer
					+ "\n\tchannelTxRevokeClient=" + channelTxRevokeClient
					+ "\n\tchannelTxRevokeServerTemp=" + channelTxRevokeServerTemp
					+ "\n\tchannelTxRevokeClientTemp=" + channelTxRevokeClientTemp
					+ "\n\tclientKey=" + clientKey + "\n\tclientKeyDeterministic="
					+ clientKeyDeterministic + "\n\tserverKey=" + serverKey
					+ "\n\tserverKeyDeterministic=" + serverKeyDeterministic
					+ "\n\tgetOpeningTx()=" + getOpeningTx()
					+ "\n\tgetRefundTxServer()=" + getRefundTxServer()
					+ "\n\tgetRefundTxClient()=" + getRefundTxClient()
					+ "\n\tgetChannelTxServer()=" + getChannelTxServer()
					+ "\n\tgetChannelTxClient()=" + getChannelTxClient()
					+ "\n\tgetChannelTxServerTemp()=" + getChannelTxServerTemp()
					+ "\n\tgetChannelTxClientTemp()=" + getChannelTxClientTemp()
					+ "\n\tgetChannelTxRevokeServer()="
					+ getChannelTxRevokeServer()
					+ "\n\tgetChannelTxRevokeClient()="
					+ getChannelTxRevokeClient()
					+ "\n\tgetChannelTxRevokeServerTemp()="
					+ getChannelTxRevokeServerTemp()
					+ "\n\tgetChannelTxRevokeClientTemp()="
					+ getChannelTxRevokeClientTemp();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Channel\n\tconn=" + conn + "\n\tid=" + id + "\n\tpubKeyClient="
		+ pubKeyClient + "\n\tpubKeyServer=" + pubKeyServer
		+ "\n\tchangeAddressServer=" + changeAddressServer
		+ "\n\tchangeAddressClient=" + changeAddressClient
		+ "\n\tmasterPrivateKeyClient=" + masterPrivateKeyClient
		+ "\n\tmasterPrivateKeyServer=" + masterPrivateKeyServer
		+ "\n\tinitialAmountServer=" + initialAmountServer
		+ "\n\tinitialAmountClient=" + initialAmountClient
		+ "\n\tamountServer=" + amountServer + "\n\tamountClient="
		+ amountClient + "\n\ttimestampOpen=" + timestampOpen
		+ "\n\ttimestampClose=" + timestampClose
		+ "\n\ttimestampForceClose=" + timestampForceClose
		+ "\n\topeningTxID=" + openingTxID + "\n\trefundTxServerID="
		+ refundTxServerID + "\n\trefundTxClientID=" + refundTxClientID
		+ "\n\tchannelTxServerID=" + channelTxServerID
		+ "\n\tchannelTxClientID=" + channelTxClientID
		+ "\n\tchannelTxServerTempID=" + channelTxServerTempID
		+ "\n\tchannelTxClientTempID=" + channelTxClientTempID
		+ "\n\tchannelTxRevokeServerID=" + channelTxRevokeServerID
		+ "\n\tchannelTxRevokeClientID=" + channelTxRevokeClientID
		+ "\n\tchannelTxRevokeServerTempID="
		+ channelTxRevokeServerTempID
		+ "\n\tchannelTxRevokeClientTempID="
		+ channelTxRevokeClientTempID + "\n\thasOpenPayments="
		+ hasOpenPayments + "\n\testablishPhase=" + establishPhase
		+ "\n\tpaymentPhase=" + paymentPhase + "\n\tisReady=" + isReady
		+ "\n\tkeyChainDepth=" + keyChainDepth + "\n\tkeyChainChild="
		+ keyChainChild + "\n\tmasterChainDepth=" + masterChainDepth
		+ "\n\topeningTxHash=" + openingTxHash
		+ "\n\topeningTxChanged=" + openingTxChanged
		+ "\n\trefundTxServerChanged=" + refundTxServerChanged
		+ "\n\trefundTxClientChanged=" + refundTxClientChanged
		+ "\n\tchannelTxServerChanged=" + channelTxServerChanged
		+ "\n\tchannelTxClientChanged=" + channelTxClientChanged
		+ "\n\tchannelTxServerTempChanged="
		+ channelTxServerTempChanged
		+ "\n\tchannelTxClientTempChanged="
		+ channelTxClientTempChanged
		+ "\n\tchannelTxRevokeServerChanged="
		+ channelTxRevokeServerChanged
		+ "\n\tchannelTxRevokeClientChanged="
		+ channelTxRevokeClientChanged
		+ "\n\tchannelTxRevokeServerTempChanged="
		+ channelTxRevokeServerTempChanged
		+ "\n\tchannelTxRevokeClientTempChanged="
		+ channelTxRevokeClientTempChanged + "\n\topeningTx="
		+ openingTx + "\n\trefundTxServer=" + refundTxServer
		+ "\n\trefundTxClient=" + refundTxClient
		+ "\n\tchannelTxServer=" + channelTxServer
		+ "\n\tchannelTxClient=" + channelTxClient
		+ "\n\tchannelTxServerTemp=" + channelTxServerTemp
		+ "\n\tchannelTxClientTemp=" + channelTxClientTemp
		+ "\n\tchannelTxRevokeServer=" + channelTxRevokeServer
		+ "\n\tchannelTxRevokeClient=" + channelTxRevokeClient
		+ "\n\tchannelTxRevokeServerTemp=" + channelTxRevokeServerTemp
		+ "\n\tchannelTxRevokeClientTemp=" + channelTxRevokeClientTemp
		+ "\n\tclientKey=" + clientKey + "\n\tclientKeyDeterministic="
		+ clientKeyDeterministic + "\n\tserverKey=" + serverKey
		+ "\n\tserverKeyDeterministic=" + serverKeyDeterministic;
	}

	
}
