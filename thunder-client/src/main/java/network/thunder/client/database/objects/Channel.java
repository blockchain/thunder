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
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.KeyDerivation;
import network.thunder.client.etc.SideConstants;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: We probably want very flexible rules for channels in the future. Currently, these rules are set as Constants in Constants.class.
 * Add all those constants as columns to the Channels table.
 * Seeing the channel table growing, it will become less and less clear.
 * This change also has to be done on client side.
 * Adding it here will mean that we have constant terms for the entirety of the channel, but with different terms for different channels.
 * We can also - at a later point - include the possibility to change these terms with a protocol change.
 * <p>
 * TODO: Add a 'Domain' field to the channels table (and consequently to this Class).
 * This will allow the choice of the payment hub, and will also prepare for Multi-Hop solutions.
 * <p>
 * Other necessary work for Multi-Hop:
 * - Have some system for routing payments in place
 * - Have symmetric channels (requires a no-trust solution)
 * - Have a INFO API, such that
 * - Users can see the terms, at which they will open a channel
 * - Other Payment Hubs can check, to which payment hubs this hub is connected to
 * <p>
 * Communications for Payments from one Channel to another Channel will still follow the same rules we have in place currently.
 */

public class Channel {

	public Connection conn;

	private int id;

	private String pubKeyClient;
	private String pubKeyServer;
	private String changeAddressServer;
	private String changeAddressClient;
	private String masterPrivateKeyClient;
	private String masterPrivateKeyServer;
	private long initialAmountServer;
	private long initialAmountClient;
	private long amountServer;
	private long amountClient;
	private int timestampOpen;
	private int timestampClose;
	private int timestampForceClose;
	private int openingTxID;
	private int refundTxServerID;
	private int refundTxClientID;
	private int channelTxServerID;
	private int channelTxClientID;
	private int channelTxServerTempID;
	private int channelTxClientTempID;
	private int channelTxRevokeServerID;
	private int channelTxRevokeClientID;
	private int channelTxRevokeServerTempID;
	private int channelTxRevokeClientTempID;
	private boolean hasOpenPayments;
	private int establishPhase;
	private int paymentPhase;
	private boolean isReady;

	private int keyChainDepth;
	private int keyChainChild;
	private int masterChainDepth;

	private String openingTxHash;

	private boolean openingTxChanged;
	private boolean refundTxServerChanged;
	private boolean refundTxClientChanged;
	private boolean channelTxServerChanged;
	private boolean channelTxClientChanged;
	private boolean channelTxServerTempChanged;
	private boolean channelTxClientTempChanged;
	private boolean channelTxRevokeServerChanged;
	private boolean channelTxRevokeClientChanged;
	private boolean channelTxRevokeServerTempChanged;
	private boolean channelTxRevokeClientTempChanged;

	private Transaction openingTx;
	private Transaction refundTxServer;
	private Transaction refundTxClient;
	private Transaction channelTxServer;
	private Transaction channelTxClient;
	private Transaction channelTxServerTemp;
	private Transaction channelTxClientTemp;
	private Transaction channelTxRevokeServer;
	private Transaction channelTxRevokeClient;
	private Transaction channelTxRevokeServerTemp;
	private Transaction channelTxRevokeClientTemp;

	private ECKey clientKey;
	private DeterministicKey clientKeyDeterministic;
	private ECKey serverKey;
	private DeterministicKey serverKeyDeterministic;

	public Channel () {
	}

	public Channel (ResultSet result) throws SQLException {
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

	public long getAmountClient () {
		return amountClient;
	}

	public void setAmountClient (long amountYou) {
		this.amountClient = amountYou;
	}

	public long getAmountServer () {
		return amountServer;
	}

	public void setAmountServer (long amountMe) {
		this.amountServer = amountMe;
	}

	public String getChangeAddressClient () {
		return changeAddressClient;
	}

	public void setChangeAddressClient (String changeAddressClient) {
		this.changeAddressClient = changeAddressClient;
	}

	public Address getChangeAddressClientAsAddress () throws AddressFormatException {
		return new Address(Constants.getNetwork(), this.getChangeAddressClient());
	}

	public String getChangeAddressServer () {
		return changeAddressServer;
	}

	public void setChangeAddressServer (String changeAddressServer) {
		this.changeAddressServer = changeAddressServer;
	}

	public Address getChangeAddressServerAsAddress () throws AddressFormatException {
		return new Address(Constants.getNetwork(), this.getChangeAddressServer());
	}

	public ArrayList<Transaction> getChangedTransactions () {
		ArrayList<Transaction> list = new ArrayList<Transaction>();
		if (openingTxChanged) {
			list.add(openingTx);
		}
		if (refundTxServerChanged) {
			list.add(refundTxServer);
		}
		if (refundTxClientChanged) {
			list.add(refundTxClient);
		}
		if (channelTxServerChanged) {
			list.add(channelTxServer);
		}
		if (channelTxClientChanged) {
			list.add(channelTxClient);
		}
		if (channelTxServerTempChanged) {
			list.add(channelTxServerTemp);
		}
		if (channelTxClientTempChanged) {
			list.add(channelTxClientTemp);
		}
		if (channelTxRevokeServerChanged) {
			list.add(channelTxRevokeServer);
		}
		if (channelTxRevokeClientChanged) {
			list.add(channelTxRevokeClient);
		}
		if (channelTxRevokeServerTempChanged) {
			list.add(channelTxRevokeServerTemp);
		}
		if (channelTxRevokeClientTempChanged) {
			list.add(channelTxRevokeClientTemp);
		}

		return list;
	}

	public Transaction getChannelTxClient () throws SQLException {
		if (channelTxClient == null) {
			channelTxClient = MySQLConnection.getTransaction(conn, channelTxClientID);
		}
		return channelTxClient;
	}

	public void setChannelTxClient (Transaction channelTxYou) {
		channelTxClientChanged = true;
		this.channelTxClient = channelTxYou;
	}

	public int getChannelTxClientID () {
		return channelTxClientID;
	}

	public void setChannelTxClientID (int channelTxYouID) {
		this.channelTxClientID = channelTxYouID;
	}

	public Transaction getChannelTxClientTemp () throws SQLException {
		if (channelTxClientTemp == null) {
			channelTxClientTemp = MySQLConnection.getTransaction(conn, channelTxClientTempID);
		}
		return channelTxClientTemp;
	}

	public void setChannelTxClientTemp (Transaction channelTxYouTemp) {
		channelTxClientTempChanged = true;
		this.channelTxClientTemp = channelTxYouTemp;
	}

	public int getChannelTxClientTempID () {
		return channelTxClientTempID;
	}

	public void setChannelTxClientTempID (int channelTxYouTempID) {
		this.channelTxClientTempID = channelTxYouTempID;
	}

	public Transaction getChannelTxRevokeClient () throws SQLException {
		if (channelTxRevokeClient == null) {
			channelTxRevokeClient = MySQLConnection.getTransaction(conn, channelTxRevokeClientID);
		}
		return channelTxRevokeClient;
	}

	public void setChannelTxRevokeClient (Transaction channelTxRevokeYou) {
		channelTxRevokeClientChanged = true;
		this.channelTxRevokeClient = channelTxRevokeYou;
	}

	public int getChannelTxRevokeClientID () {
		return channelTxRevokeClientID;
	}

	public void setChannelTxRevokeClientID (int channelTxRevokeYouID) {
		this.channelTxRevokeClientID = channelTxRevokeYouID;
	}

	public Transaction getChannelTxRevokeClientTemp () throws SQLException {
		if (channelTxRevokeClientTemp == null) {
			channelTxRevokeClientTemp = MySQLConnection.getTransaction(conn, channelTxRevokeClientTempID);
		}
		return channelTxRevokeClientTemp;
	}

	public void setChannelTxRevokeClientTemp (Transaction channelTxRevokeYouTemp) {
		channelTxRevokeClientTempChanged = true;
		this.channelTxRevokeClientTemp = channelTxRevokeYouTemp;
	}

	public int getChannelTxRevokeClientTempID () {
		return channelTxRevokeClientTempID;
	}

	public void setChannelTxRevokeClientTempID (int channelTxRevokeClientTempID) {
		this.channelTxRevokeClientTempID = channelTxRevokeClientTempID;
	}

	public Transaction getChannelTxRevokeServer () throws SQLException {
		if (channelTxRevokeServer == null) {
			channelTxRevokeServer = MySQLConnection.getTransaction(conn, channelTxRevokeServerID);
		}
		return channelTxRevokeServer;
	}

	public void setChannelTxRevokeServer (Transaction channelTxRevokeMe) {
		channelTxRevokeServerChanged = true;
		this.channelTxRevokeServer = channelTxRevokeMe;
	}

	public int getChannelTxRevokeServerID () {
		return channelTxRevokeServerID;
	}

	public void setChannelTxRevokeServerID (int channelTxRevokeMeID) {
		this.channelTxRevokeServerID = channelTxRevokeMeID;
	}

	public Transaction getChannelTxRevokeServerTemp () throws SQLException {
		if (channelTxRevokeServerTemp == null) {
			channelTxRevokeServerTemp = MySQLConnection.getTransaction(conn, channelTxRevokeServerTempID);
		}
		return channelTxRevokeServerTemp;
	}

	public void setChannelTxRevokeServerTemp (Transaction channelTxRevokeMeTemp) {
		channelTxRevokeServerTempChanged = true;
		this.channelTxRevokeServerTemp = channelTxRevokeMeTemp;
	}

	public int getChannelTxRevokeServerTempID () {
		return channelTxRevokeServerTempID;
	}

	public void setChannelTxRevokeServerTempID (int channelTxRevokeServerTempID) {
		this.channelTxRevokeServerTempID = channelTxRevokeServerTempID;
	}

	public Transaction getChannelTxServer () throws SQLException {
		if (channelTxServer == null) {
			channelTxServer = MySQLConnection.getTransaction(conn, channelTxServerID);
		}
		return channelTxServer;
	}

	public void setChannelTxServer (Transaction channelTxMe) {
		channelTxServerChanged = true;
		this.channelTxServer = channelTxMe;
	}

	public int getChannelTxServerID () {
		return channelTxServerID;
	}

	public void setChannelTxServerID (int channelTxMeID) {
		this.channelTxServerID = channelTxMeID;
	}

	public Transaction getChannelTxServerTemp () throws SQLException {
		if (channelTxServerTemp == null) {
			channelTxServerTemp = MySQLConnection.getTransaction(conn, channelTxServerTempID);
		}
		return channelTxServerTemp;
	}

	public void setChannelTxServerTemp (Transaction channelTxMeTemp) {
		channelTxServerTempChanged = true;
		this.channelTxServerTemp = channelTxMeTemp;
	}

	public int getChannelTxServerTempID () {
		return channelTxServerTempID;
	}

	public void setChannelTxServerTempID (int channelTxMeTempID) {
		this.channelTxServerTempID = channelTxMeTempID;
	}

	public DeterministicKey getClientKeyDeterministic () {
		if (clientKeyDeterministic == null) {
			clientKeyDeterministic = DeterministicKey.deserializeB58(this.getMasterPrivateKeyClient(), Constants.getNetwork());
			//			clientKeyDeterministic = DeterministicKey.deserializeB58(null, masterPrivateKeyClient);
		}
		return clientKeyDeterministic;
	}

	public ECKey getClientKeyOnClient () {
		if (clientKey == null) {
			clientKey = getClientKeyDeterministic();
		}
		return clientKey;
	}

	public ECKey getClientKeyOnServer () {
		if (clientKey == null) {
			clientKey = ECKey.fromPublicOnly(Tools.stringToByte(pubKeyClient));
		}
		return clientKey;
	}

	public int getEstablishPhase () {
		return establishPhase;
	}

	public void setEstablishPhase (int establishPhase) {
		this.establishPhase = establishPhase;
	}

	public boolean getHasOpenPayments () {
		return hasOpenPayments;
	}

	public void setHasOpenPayments (boolean hasOpenPayments) {
		this.hasOpenPayments = hasOpenPayments;
	}

	public DeterministicHierarchy getHierachyClient () {
		DeterministicKey masterKey = DeterministicKey.deserializeB58(getMasterPrivateKeyClient(), Constants.getNetwork());
		return new DeterministicHierarchy(masterKey);
	}

	public DeterministicHierarchy getHierachyServer () {
		DeterministicKey masterKey = DeterministicKey.deserializeB58(getMasterPrivateKeyServer(), Constants.getNetwork());
		return new DeterministicHierarchy(masterKey);
	}

	public int getId () {
		return id;
	}

	public void setId (int id) {
		this.id = id;
	}

	public long getInitialAmountClient () {
		return initialAmountClient;
	}

	public void setInitialAmountClient (long initialAmountYou) {
		this.initialAmountClient = initialAmountYou;
	}

	public long getInitialAmountServer () {
		return initialAmountServer;
	}

	public void setInitialAmountServer (long initialAmountMe) {
		this.initialAmountServer = initialAmountMe;
	}

	public int getKeyChainChild () {
		return keyChainChild;
	}

	public void setKeyChainChild (int keyChainChild) {
		this.keyChainChild = keyChainChild;
	}

	public int getKeyChainDepth () {
		return keyChainDepth;
	}

	public void setKeyChainDepth (int keyChainDepth) {
		this.keyChainDepth = keyChainDepth;
	}

	public int getMasterChainDepth () {
		return masterChainDepth;
	}

	public void setMasterChainDepth (int masterChainDepth) {
		this.masterChainDepth = masterChainDepth;
	}

	public String getMasterPrivateKeyClient () {
		return masterPrivateKeyClient;
	}

	public void setMasterPrivateKeyClient (String masterPrivateKeyYou) {
		this.masterPrivateKeyClient = masterPrivateKeyYou;
	}

	public String getMasterPrivateKeyServer () {
		return masterPrivateKeyServer;
	}

	public void setMasterPrivateKeyServer (String masterPrivateKeyMe) {
		this.masterPrivateKeyServer = masterPrivateKeyMe;
	}

	public Transaction getOpeningTx () throws SQLException {
		if (openingTx == null) {
			openingTx = MySQLConnection.getTransaction(conn, openingTxID);
		}
		return openingTx;
	}

	public void setOpeningTx (Transaction openingTx) {
		openingTxChanged = true;
		this.openingTx = openingTx;
	}

	public String getOpeningTxHash () {
		return openingTxHash;
	}

	public void setOpeningTxHash (String openingTxHash) {
		this.openingTxHash = openingTxHash;
	}

	public int getOpeningTxID () {
		return openingTxID;
	}

	public void setOpeningTxID (int openingTxID) {
		this.openingTxID = openingTxID;
	}

	public int getPaymentPhase () {
		return paymentPhase;
	}

	public void setPaymentPhase (int paymentPhase) {
		this.paymentPhase = paymentPhase;
	}

	public String getPubKeyClient () {
		return pubKeyClient;
	}

	public void setPubKeyClient (String pubKey) {
		this.pubKeyClient = pubKey;
	}

	public String getPubKeyServer () {
		return pubKeyServer;
	}

	public void setPubKeyServer (String pubKeyServer) {
		this.pubKeyServer = pubKeyServer;
	}

	public Transaction getRefundTxClient () throws SQLException {
		if (refundTxClient == null) {
			refundTxClient = MySQLConnection.getTransaction(conn, refundTxClientID);
		}
		return refundTxClient;
	}

	public void setRefundTxClient (Transaction refundTxYou) {
		refundTxClientChanged = true;
		this.refundTxClient = refundTxYou;
	}

	public int getRefundTxClientID () {
		return refundTxClientID;
	}

	public void setRefundTxClientID (int refundTxYouID) {
		this.refundTxClientID = refundTxYouID;
	}

	public Transaction getRefundTxServer () throws SQLException {
		if (refundTxServer == null) {
			refundTxServer = MySQLConnection.getTransaction(conn, refundTxServerID);
		}
		return refundTxServer;
	}

	public void setRefundTxServer (Transaction refundTxMe) {
		refundTxServerChanged = true;
		this.refundTxServer = refundTxMe;
	}

	public int getRefundTxServerID () {
		return refundTxServerID;
	}

	public void setRefundTxServerID (int refundTxMeID) {
		this.refundTxServerID = refundTxMeID;
	}

	public DeterministicKey getServerKeyDeterministic () {
		if (serverKeyDeterministic == null) {
			serverKeyDeterministic = DeterministicKey.deserializeB58(this.getMasterPrivateKeyServer(), Constants.getNetwork());
			//			serverKeyDeterministic = DeterministicKey.deserializeB58(null, masterPrivateKeyServer);
		}
		return serverKeyDeterministic;
	}

	public ECKey getServerKeyOnClient () {
		if (serverKey == null) {
			//			serverKey = DeterministicKey.deserializeB58(masterPrivateKeyMe, Constants.getNetwork());
			serverKey = ECKey.fromPublicOnly(Tools.stringToByte(pubKeyServer));
		}
		return serverKey;
	}

	public ECKey getServerKeyOnServer () {
		if (serverKey == null) {
			//			serverKey = DeterministicKey.deserializeB58(masterPrivateKeyMe, Constants.getNetwork());
			serverKey = getServerKeyDeterministic();
		}
		return serverKey;
	}

	public int getTimestampClose () {
		return timestampClose;
	}

	public void setTimestampClose (int timestampClose) {
		this.timestampClose = timestampClose;
	}

	public int getTimestampForceClose () {
		return timestampForceClose;
	}

	public void setTimestampForceClose (int timestampForceClose) {
		this.timestampForceClose = timestampForceClose;
	}

	public int getTimestampOpen () {
		return timestampOpen;
	}

	public void setTimestampOpen (int timestampOpen) {
		this.timestampOpen = timestampOpen;
	}

	public int getTimestampRefunds () {
		return this.timestampClose - Constants.SECURITY_TIME_WINDOW_BEFORE_CHANNEL_ENDS;
	}

	public ArrayList<Integer> getTransactionIDs () {
		ArrayList<Integer> list = new ArrayList<Integer>();

		if (openingTxID != 0) {
			list.add(openingTxID);
		}
		if (refundTxServerID != 0) {
			list.add(refundTxServerID);
		}
		if (refundTxClientID != 0) {
			list.add(refundTxClientID);
		}
		if (channelTxServerID != 0) {
			list.add(channelTxServerID);
		}
		if (channelTxClientID != 0) {
			list.add(channelTxClientID);
		}
		if (channelTxServerTempID != 0) {
			list.add(channelTxServerTempID);
		}
		if (channelTxClientTempID != 0) {
			list.add(channelTxClientTempID);
		}
		if (channelTxRevokeServerID != 0) {
			list.add(channelTxRevokeServerID);
		}
		if (channelTxRevokeClientID != 0) {
			list.add(channelTxRevokeClientID);
		}
		if (channelTxRevokeServerTempID != 0) {
			list.add(channelTxRevokeServerTempID);
		}
		if (channelTxRevokeClientTempID != 0) {
			list.add(channelTxRevokeClientTempID);
		}

		return list;
	}

	public boolean isReady () {
		return isReady;
	}

	public void setReady (boolean isReady) {
		this.isReady = isReady;
	}

	public void newMasterKey (Key masterKey) throws Exception {
		System.out.println("New Masterkey: " + masterKey.privateKey + " . " + masterKey.depth + " : " + masterKey.child);
		if ((SideConstants.RUNS_ON_SERVER && getMasterPrivateKeyClient() != null) || (!SideConstants.RUNS_ON_SERVER && getMasterPrivateKeyServer() != null)) {
			/**
			 * Make sure the old masterPrivateKey is a child of this one..
			 */

			DeterministicKey key = DeterministicKey.deserializeB58(masterKey.privateKey, Constants.getNetwork());
			DeterministicHierarchy hierachy = new DeterministicHierarchy(key);

			List<ChildNumber> childList = KeyDerivation.getChildList(getMasterChainDepth() - masterKey.depth);
			DeterministicKey keyDerived = hierachy.get(childList, true, true);

			if (!KeyDerivation.compareDeterministicKeys(keyDerived, getMasterPrivateKeyClient())) {
				System.out.println(keyDerived);
				System.out.println(getMasterPrivateKeyClient());
				throw new Exception("The new masterPrivateKey is not a parent of the one we have..");
			}
		}

		if (SideConstants.RUNS_ON_SERVER) {
			setMasterPrivateKeyClient(masterKey.privateKey);
		} else {
			setMasterPrivateKeyServer(masterKey.privateKey);
		}
		setMasterChainDepth(masterKey.depth);
	}

	public void replaceCurrentTransactionsWithTemporary () throws SQLException {

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

	public void updateTransactionsToDatabase (Connection conn) throws SQLException {
		if (openingTxChanged) {
			openingTxID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(openingTx, id, openingTxID));
		}
		if (refundTxServerChanged) {
			refundTxServerID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxServer, id, refundTxServerID));
		}
		if (refundTxClientChanged) {
			refundTxClientID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(refundTxClient, id, refundTxClientID));
		}
		if (channelTxServerChanged) {
			channelTxServerID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxServer, id, channelTxServerID));
		}
		if (channelTxClientChanged) {
			channelTxClientID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxClient, id, channelTxClientID));
		}
		if (channelTxServerTempChanged) {
			channelTxServerTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxServerTemp, id, channelTxServerTempID));
		}
		if (channelTxClientTempChanged) {
			channelTxClientTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxClientTemp, id, channelTxClientTempID));
		}
		if (channelTxRevokeServerChanged) {
			channelTxRevokeServerID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxRevokeServer, id, channelTxRevokeServerID));
		}
		if (channelTxRevokeClientChanged) {
			channelTxRevokeClientID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxRevokeClient, id, channelTxRevokeClientID));
		}
		if (channelTxRevokeServerTempChanged) {
			channelTxRevokeServerTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxRevokeServerTemp, id,
					channelTxRevokeServerTempID));
		}
		if (channelTxRevokeClientTempChanged) {
			channelTxRevokeClientTempID = MySQLConnection.addOrUpdateTransaction(conn, new TransactionWrapper(channelTxRevokeClientTemp, id,
					channelTxRevokeClientTempID));
		}

	}

}
