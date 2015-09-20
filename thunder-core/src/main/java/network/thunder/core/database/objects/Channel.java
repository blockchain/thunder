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

import network.thunder.core.communication.objects.subobjects.RevocationHash;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.KeyDerivation;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

// TODO: Auto-generated Javadoc

/*
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

/**
 * The Class Channel.
 */
public class Channel {

	/* Channel id
	 */
	private int id;
	private int nodeId;

	/*
	 * We save the pubkeys as Strings currently.
	 * These are the main keys used to sign each channel transaction.
	 */
	private String pubKeyClient;
	private String pubKeyServer;

	/* These addresses are used to pay out the remaining amount that is not locked up in any payment.
	 */
	private Address changeAddressServer;
	private Address changeAddressClient;

	/* The master private key client.
	 */
	private String masterPrivateKeyClient;
	private String masterPrivateKeyServer;

	/*
	 * Current and initial balances.
	 * We update the current balances whenever a payment is settled or refunded.
	 * Open payments are included in the current amounts.
	 */
	private long initialAmountServer;
	private long initialAmountClient;
	private long amountServer;
	private long amountClient;

	/*
	 * Timestamps for the channel management.
	 * For now we keep the force close timestamp. It is updated when the channel changed.
	 * It is easy to keep track when to force broadcast a channel to the blockchain this way.
	 */
	private int timestampOpen;
	private int timestampForceClose;

	/*
	 * Signatures for broadcasting transactions.
	 * Escape and FastEscape Transactions are for claiming our initial funds when something goes wrong before the first commitment or if the other party
	 * tries to claim their initial funds after the first commitment.
	 */
	private ECKey.ECDSASignature escapeTxSig;
	private ECKey.ECDSASignature escapeFastTxSig;
	private ECKey.ECDSASignature channelTxSig;
	private ECKey.ECDSASignature channelTxTempSig;

	/*
	 * Upcounting version number to keep track which revocation-hash is used with which payments.
	 * We increase it, whenever we commited to a new channel.
	 */
	private int channelTxVersion;

	/*
	 * Hashes to build each channel transaction.
	 */
	private String openingTxHashServer;
	private String openingTxHashClient;

	/*
	 * The secrets for making the opening transactions.
	 * We only use them once or in case the other party tries to cheat on us.
	 */
	private String openingSecretServer;
	private String openingSecretClient;

	/**
	 * Enum to mark the different phases.
	 * <p>
	 * These are necessary, as we save the state back to the database after each communication.
	 */
	private Phase phase;

	public enum Phase {
		NEUTRAL("0"),
		ESTABLISH_REQUESTED("11"),
		ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION("12"),
		PAYMENT_REQUESTED("21"),
		UPDATE_REQUESTED("31"),
		CLOSED("50");

		private String value;

		private Phase (String value) {
			this.value = value;
		}

		public String getValue () {
			return value;
		}

	}

	/*
	 * Determines if the channel is ready to make/receive payments.
	 * We set this to true once the opening txs have enough confirmations.
	 * We set this to false if the channel is closed.
	 */
	private boolean isReady;

	/*
	 * Keeping track of the revocation hashes we gave out.
	 * When we open the channel we set the depth to some high value and decrease it every X hours.
	 * Whenever we commit to a new version of the channel, we use a new child derived from the depth.
	 */
	private int keyChainDepth;
	private int keyChainChild;

	/*
	 * We keep track of the key chain of the other party.
	 * Doing so allows us to recreate and check old keys, as we know the depth of the current key we hold without poking around in the dark.
	 */
	private int masterChainDepth;

	/*
	 * Keys used for all channel transactions.
	 * These are the keys used for the 2-of-2 multisig of the opening transactions.
	 * We need them for any updates to sign and to check the signature of the other party.
	 */
	private ECKey clientKey;
	private DeterministicKey clientKeyDeterministic;
	private ECKey serverKey;
	private DeterministicKey serverKeyDeterministic;

	public String getOpeningTxHashServer () {
		return openingTxHashServer;
	}

	public void setOpeningTxHashServer (String openingTxHashServer) {
		this.openingTxHashServer = openingTxHashServer;
	}

	public String getOpeningTxHashClient () {
		return openingTxHashClient;
	}

	public void setOpeningTxHashClient (String openingTxHashClient) {
		this.openingTxHashClient = openingTxHashClient;
	}

	public String getOpeningSecretServer () {
		return openingSecretServer;
	}

	public void setOpeningSecretServer (String openingSecretServer) {
		this.openingSecretServer = openingSecretServer;
	}

	public String getOpeningSecretClient () {
		return openingSecretClient;
	}

	public void setOpeningSecretClient (String openingSecretClient) {
		this.openingSecretClient = openingSecretClient;
	}

	public Phase getPhase () {
		return phase;
	}

	public void setPhase (Phase phase) {
		this.phase = phase;
	}

	public void setIsReady (boolean isReady) {
		this.isReady = isReady;
	}

	/**
	 * Instantiates a new channel.
	 *
	 * @param result the result
	 * @throws SQLException the SQL exception
	 */
	public Channel (ResultSet result) throws SQLException {
		this.setId(result.getInt("id"));
		this.setNodeId(result.getInt("node_id"));

		this.setPubKeyClient(result.getString("pub_key_client"));
		this.setPubKeyServer(result.getString("pub_key_server"));
		try {
			this.setChangeAddressClient(new Address(Constants.getNetwork(), result.getString("change_address_client")));
			this.setChangeAddressServer(new Address(Constants.getNetwork(), result.getString("change_address_server")));
		} catch (AddressFormatException e) {
			throw new RuntimeException(e);
		}
		this.setMasterPrivateKeyServer(result.getString("master_priv_key_server"));
		this.setMasterPrivateKeyClient(result.getString("master_priv_key_client"));

		this.setInitialAmountServer(result.getLong("initial_amount_server"));
		this.setInitialAmountClient(result.getLong("initial_amount_client"));
		this.setAmountServer(result.getLong("amount_server"));
		this.setAmountClient(result.getLong("amount_client"));

		this.setTimestampOpen(result.getInt("timestamp_open"));
		this.setTimestampForceClose(result.getInt("timestamp_force_close"));

		this.setKeyChainChild(result.getInt("key_chain_child"));
		this.setKeyChainDepth(result.getInt("key_chain_depth"));
		this.setMasterChainDepth(result.getInt("master_chain_depth"));

		this.setOpeningTxHashClient(result.getString("opening_tx_hash_client"));
		this.setOpeningTxHashClient(result.getString("opening_tx_hash_server"));
		this.setOpeningSecretClient(result.getString("opening_secret_client"));
		this.setOpeningSecretClient(result.getString("opening_secret_server"));

		this.setEscapeTxSig(ECKey.ECDSASignature.decodeFromDER(result.getBytes("escape_tx_sig")));
		this.setEscapeFastTxSig(ECKey.ECDSASignature.decodeFromDER(result.getBytes("escape_fast_tx_sig")));
		this.setChannelTxSig(ECKey.ECDSASignature.decodeFromDER(result.getBytes("escape_fast_tx_sig")));
		this.setChannelTxTempSig(ECKey.ECDSASignature.decodeFromDER(result.getBytes("escape_fast_tx_sig")));

		this.setChannelTxVersion(result.getInt("channel_tx_version"));
		this.setPhase(Phase.valueOf(result.getString("phase")));
		this.setReady(Tools.intToBool(result.getInt("is_ready")));

	}

	/**
	 * Gets the amount client.
	 *
	 * @return the amount client
	 */
	public long getAmountClient () {
		return amountClient;
	}

	/**
	 * Sets the amount client.
	 *
	 * @param amountYou the new amount client
	 */
	public void setAmountClient (long amountYou) {
		this.amountClient = amountYou;
	}

	/**
	 * Gets the amount server.
	 *
	 * @return the amount server
	 */
	public long getAmountServer () {
		return amountServer;
	}

	/**
	 * Sets the amount server.
	 *
	 * @param amountMe the new amount server
	 */
	public void setAmountServer (long amountMe) {
		this.amountServer = amountMe;
	}

	/**
	 * Gets the change address client.
	 *
	 * @return the change address client
	 */
	public Address getChangeAddressClient () {
		return changeAddressClient;
	}

	/**
	 * Sets the change address client.
	 *
	 * @param changeAddressClient the new change address client
	 */
	public void setChangeAddressClient (Address changeAddressClient) {
		this.changeAddressClient = changeAddressClient;
	}

	/**
	 * Gets the change address server.
	 *
	 * @return the change address server
	 */
	public Address getChangeAddressServer () {
		return changeAddressServer;
	}

	/**
	 * Sets the change address server.
	 *
	 * @param changeAddressServer the new change address server
	 */
	public void setChangeAddressServer (Address changeAddressServer) {
		this.changeAddressServer = changeAddressServer;
	}

	/**
	 * Gets the client key deterministic.
	 *
	 * @return the client key deterministic
	 */
	public DeterministicKey getClientKeyDeterministic () {
		if (clientKeyDeterministic == null) {
			clientKeyDeterministic = DeterministicKey.deserializeB58(this.getMasterPrivateKeyClient(), Constants.getNetwork());
			//			clientKeyDeterministic = DeterministicKey.deserializeB58(null, masterPrivateKeyClient);
		}
		return clientKeyDeterministic;
	}

	/**
	 * Gets the client key on client.
	 *
	 * @return the client key on client
	 */
	public ECKey getClientKeyOnClient () {
		if (clientKey == null) {
			clientKey = getClientKeyDeterministic();
		}
		return clientKey;
	}

	/**
	 * Gets the client key on server.
	 *
	 * @return the client key on server
	 */
	public ECKey getClientKeyOnServer () {
		if (clientKey == null) {
			clientKey = ECKey.fromPublicOnly(Tools.stringToByte(pubKeyClient));
		}
		return clientKey;
	}

	/**
	 * Gets the hierachy client.
	 *
	 * @return the hierachy client
	 */
	public DeterministicHierarchy getHierachyClient () {
		DeterministicKey masterKey = DeterministicKey.deserializeB58(getMasterPrivateKeyClient(), Constants.getNetwork());
		return new DeterministicHierarchy(masterKey);
	}

	/**
	 * Gets the hierachy server.
	 *
	 * @return the hierachy server
	 */
	public DeterministicHierarchy getHierachyServer () {
		DeterministicKey masterKey = DeterministicKey.deserializeB58(getMasterPrivateKeyServer(), Constants.getNetwork());
		return new DeterministicHierarchy(masterKey);
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

	/**
	 * Gets the initial amount client.
	 *
	 * @return the initial amount client
	 */
	public long getInitialAmountClient () {
		return initialAmountClient;
	}

	/**
	 * Sets the initial amount client.
	 *
	 * @param initialAmountYou the new initial amount client
	 */
	public void setInitialAmountClient (long initialAmountYou) {
		this.initialAmountClient = initialAmountYou;
	}

	/**
	 * Gets the initial amount server.
	 *
	 * @return the initial amount server
	 */
	public long getInitialAmountServer () {
		return initialAmountServer;
	}

	/**
	 * Sets the initial amount server.
	 *
	 * @param initialAmountMe the new initial amount server
	 */
	public void setInitialAmountServer (long initialAmountMe) {
		this.initialAmountServer = initialAmountMe;
	}

	/**
	 * Gets the key chain child.
	 *
	 * @return the key chain child
	 */
	public int getKeyChainChild () {
		return keyChainChild;
	}

	/**
	 * Sets the key chain child.
	 *
	 * @param keyChainChild the new key chain child
	 */
	public void setKeyChainChild (int keyChainChild) {
		this.keyChainChild = keyChainChild;
	}

	/**
	 * Gets the key chain depth.
	 *
	 * @return the key chain depth
	 */
	public int getKeyChainDepth () {
		return keyChainDepth;
	}

	/**
	 * Sets the key chain depth.
	 *
	 * @param keyChainDepth the new key chain depth
	 */
	public void setKeyChainDepth (int keyChainDepth) {
		this.keyChainDepth = keyChainDepth;
	}

	/**
	 * Gets the master chain depth.
	 *
	 * @return the master chain depth
	 */
	public int getMasterChainDepth () {
		return masterChainDepth;
	}

	/**
	 * Sets the master chain depth.
	 *
	 * @param masterChainDepth the new master chain depth
	 */
	public void setMasterChainDepth (int masterChainDepth) {
		this.masterChainDepth = masterChainDepth;
	}

	/**
	 * Gets the master private key client.
	 *
	 * @return the master private key client
	 */
	public String getMasterPrivateKeyClient () {
		return masterPrivateKeyClient;
	}

	/**
	 * Sets the master private key client.
	 *
	 * @param masterPrivateKeyYou the new master private key client
	 */
	public void setMasterPrivateKeyClient (String masterPrivateKeyYou) {
		this.masterPrivateKeyClient = masterPrivateKeyYou;
	}

	/**
	 * Gets the master private key server.
	 *
	 * @return the master private key server
	 */
	public String getMasterPrivateKeyServer () {
		return masterPrivateKeyServer;
	}

	/**
	 * Sets the master private key server.
	 *
	 * @param masterPrivateKeyMe the new master private key server
	 */
	public void setMasterPrivateKeyServer (String masterPrivateKeyMe) {
		this.masterPrivateKeyServer = masterPrivateKeyMe;
	}

	/**
	 * Gets the pub key client.
	 *
	 * @return the pub key client
	 */
	public String getPubKeyClient () {
		return pubKeyClient;
	}

	/**
	 * Sets the pub key client.
	 *
	 * @param pubKey the new pub key client
	 */
	public void setPubKeyClient (String pubKey) {
		this.pubKeyClient = pubKey;
	}

	/**
	 * Gets the pub key server.
	 *
	 * @return the pub key server
	 */
	public String getPubKeyServer () {
		return pubKeyServer;
	}

	/**
	 * Sets the pub key server.
	 *
	 * @param pubKeyServer the new pub key server
	 */
	public void setPubKeyServer (String pubKeyServer) {
		this.pubKeyServer = pubKeyServer;
	}

	/**
	 * Gets the server key deterministic.
	 *
	 * @return the server key deterministic
	 */
	public DeterministicKey getServerKeyDeterministic () {
		if (serverKeyDeterministic == null) {
			serverKeyDeterministic = DeterministicKey.deserializeB58(this.getMasterPrivateKeyServer(), Constants.getNetwork());
			//			serverKeyDeterministic = DeterministicKey.deserializeB58(null, masterPrivateKeyServer);
		}
		return serverKeyDeterministic;
	}

	/**
	 * Gets the server key on client.
	 *
	 * @return the server key on client
	 */
	public ECKey getServerKeyOnClient () {
		if (serverKey == null) {
			serverKey = ECKey.fromPublicOnly(Tools.stringToByte(pubKeyServer));
		}
		return serverKey;
	}

	/**
	 * Gets the server key on server.
	 *
	 * @return the server key on server
	 */
	public ECKey getServerKeyOnServer () {
		if (serverKey == null) {
			serverKey = getServerKeyDeterministic();
		}
		return serverKey;
	}

	/**
	 * Gets the timestamp force close.
	 *
	 * @return the timestamp force close
	 */
	public int getTimestampForceClose () {
		return timestampForceClose;
	}

	/**
	 * Sets the timestamp force close.
	 *
	 * @param timestampForceClose the new timestamp force close
	 */
	public void setTimestampForceClose (int timestampForceClose) {
		this.timestampForceClose = timestampForceClose;
	}

	/**
	 * Gets the timestamp open.
	 *
	 * @return the timestamp open
	 */
	public int getTimestampOpen () {
		return timestampOpen;
	}

	/**
	 * Sets the timestamp open.
	 *
	 * @param timestampOpen the new timestamp open
	 */
	public void setTimestampOpen (int timestampOpen) {
		this.timestampOpen = timestampOpen;
	}

	/**
	 * Checks if is ready.
	 *
	 * @return true, if is ready
	 */
	public boolean isReady () {
		return isReady;
	}

	/**
	 * Sets the ready.
	 *
	 * @param isReady the new ready
	 */
	public void setReady (boolean isReady) {
		this.isReady = isReady;
	}

	/**
	 * New master key.
	 *
	 * @param masterKey the master key
	 * @throws Exception the exception
	 */
	public void newMasterKey (RevocationHash masterKey) throws Exception {
		if (getMasterPrivateKeyClient() != null) {
			/*
			 * Make sure the old masterPrivateKey is a child of this one..
			 */

			DeterministicKey key = DeterministicKey.deserializeB58(masterKey.privateKey, Constants.getNetwork());
			DeterministicHierarchy hierachy = new DeterministicHierarchy(key);

			List<ChildNumber> childList = KeyDerivation.getChildList(getMasterChainDepth() - masterKey.depth);
			DeterministicKey keyDerived = hierachy.get(childList, true, true);

			if (!KeyDerivation.compareDeterministicKeys(keyDerived, getMasterPrivateKeyClient())) {
				throw new Exception("The new masterPrivateKey is not a parent of the one we have..");
			}
		}

		setMasterPrivateKeyClient(masterKey.getSecretAsString());
		setMasterChainDepth(masterKey.getDepth());
	}

	public ECKey.ECDSASignature getEscapeTxSig () {
		return escapeTxSig;
	}

	public void setEscapeTxSig (ECKey.ECDSASignature escapeTxSig) {
		this.escapeTxSig = escapeTxSig;
	}

	public ECKey.ECDSASignature getEscapeFastTxSig () {
		return escapeFastTxSig;
	}

	public void setEscapeFastTxSig (ECKey.ECDSASignature escapeFastTxSig) {
		this.escapeFastTxSig = escapeFastTxSig;
	}

	public ECKey.ECDSASignature getChannelTxSig () {
		return channelTxSig;
	}

	public void setChannelTxSig (ECKey.ECDSASignature channelTxSig) {
		this.channelTxSig = channelTxSig;
	}

	public ECKey.ECDSASignature getChannelTxTempSig () {
		return channelTxTempSig;
	}

	public void setChannelTxTempSig (ECKey.ECDSASignature channelTxTempSig) {
		this.channelTxTempSig = channelTxTempSig;
	}

	public int getChannelTxVersion () {
		return channelTxVersion;
	}

	public void setChannelTxVersion (int channelTxVersion) {
		this.channelTxVersion = channelTxVersion;
	}

	public int getNodeId () {
		return nodeId;
	}

	public void setNodeId (int nodeId) {
		this.nodeId = nodeId;
	}
}
