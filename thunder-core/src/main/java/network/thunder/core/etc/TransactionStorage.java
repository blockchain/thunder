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
package network.thunder.core.etc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import network.thunder.server.database.MySQLConnection;
import network.thunder.server.database.objects.Channel;
import network.thunder.server.database.objects.Payment;
import org.bitcoinj.core.*;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.script.Script;

import java.nio.channels.NotYetConnectedException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;

// TODO: Auto-generated Javadoc

/**
 * The Class TransactionStorage.
 */
public class TransactionStorage {

	/**
	 * The instance.
	 */
	public static TransactionStorage instance;
	/**
	 * The peer group.
	 */
	public PeerGroup peerGroup;
	/**
	 * TODO:
	 * <p>
	 * (1) Have a way to ensure transactions are broadcasted in time
	 * (2) Ensure these transactions are stored in the databank
	 * <p>
	 * <p>
	 * For now, we only have to save for broadcasting
	 * (1) Refund Transactions for channels that do not have a channel tx yet
	 * (2) Channel Transactions
	 * - If the channel does contain payments to the client, broadcast as soon as one times out
	 * - In any other case, broadcast at the timestamp of the revoke tx
	 * (3) Settlement/Refund/Revoke Transactions from broadcasted channels
	 * <p>
	 * Only (3) should be saved in the database, as (1) and (2) are very dynamic.
	 * <p>
	 * Furthermore, as (2) changes with every payment/update, implement a way to
	 */

	Connection conn;
	/**
	 * Map with tx hashes for channels that are not ready yet, because there are not enough confirmations.
	 * These are just for monitoring, when they hit that threshold, to set them ready.
	 */
	BiMap<Integer, String> openingTransactionHashesOfFreshChannel = HashBiMap.create();
	/**
	 * Map with tx hashes for channels that are ready.
	 * We need to check each transaction, if one of its inputs point to this hash:0.
	 * <p>
	 * If we find one, we have to make sure to check if the channel closed correctly and act accordingly.
	 */
	BiMap<Integer, String> openingTransactionHashes = HashBiMap.create();
	/**
	 * Map with tx hashes for channels that has been closed.
	 * <p>
	 * If there are no payments without revealed secret left, we can remove the hash from this list.
	 * We need to check each transaction, as we need to save any payment secrets used to claim these outputs.
	 */
	BiMap<Integer, String> channelTransactionHashes = HashBiMap.create();
	/**
	 * The additional transaction list.
	 */
	HashMap<Integer, ArrayList<TransactionTimerTask>> additionalTransactionList = new HashMap<Integer, ArrayList<TransactionTimerTask>>();
	/**
	 * The refund transaction list.
	 */
	HashMap<Integer, TransactionTimerTask> refundTransactionList = new HashMap<Integer, TransactionTimerTask>();
	/**
	 * The channel transaction list.
	 */
	HashMap<Integer, TransactionTimerTask> channelTransactionList = new HashMap<Integer, TransactionTimerTask>();
	/**
	 * The timer.
	 */
	Timer timer = new Timer();

	/**
	 * Instantiates a new transaction storage.
	 */
	private TransactionStorage () {
	}

	/**
	 * Initialize.
	 *
	 * @param conn      the conn
	 * @param peerGroup the peer group
	 * @throws SQLException the SQL exception
	 */
	public static void initialize (Connection conn, PeerGroup peerGroup) throws SQLException {
		TransactionStorage storage = new TransactionStorage();
		storage.conn = conn;
		storage.peerGroup = peerGroup;

		storage.openingTransactionHashesOfFreshChannel = MySQLConnection.getOpeningHashes(conn, false);
		storage.openingTransactionHashes = MySQLConnection.getOpeningHashes(conn, true);

		for (Channel c : MySQLConnection.getActiveChannels(conn)) {
			storage.onChannelChanged(c);
		}
		for (Channel c : MySQLConnection.getFreshChannels(conn)) {
			storage.onChannelOpened(c);
		}

		instance = storage;

	}

	/**
	 * Adds the opened channel.
	 *
	 * @param channel the channel
	 * @throws SQLException the SQL exception
	 */
	public void addOpenedChannel (Channel channel) throws SQLException {
		openingTransactionHashesOfFreshChannel.put(channel.getId(), channel.getOpeningTxHash());
		onChannelOpened(channel);
	}

	/**
	 * On channel changed.
	 *
	 * @param channel the channel
	 * @throws SQLException the SQL exception
	 */
	public void onChannelChanged (Channel channel) throws SQLException {
		//		channelTransactions.put(channel.getId(), channel);
		Transaction tx;
		if (SideConstants.RUNS_ON_SERVER) {
			tx = channel.getChannelTxServer();
		} else {
			tx = channel.getChannelTxClient();
		}

		TransactionTimerTask task = new TransactionTimerTask(peerGroup, tx);
		TransactionTimerTask oldTask = channelTransactionList.put(channel.getId(), task);
		if (oldTask != null) {
			oldTask.cancel();
		}
		timer.schedule(task, new Date(((long) channel.getTimestampForceClose()) * 1000));
	}

	/**
	 * On channel closed.
	 *
	 * @param channel     the channel
	 * @param transaction the transaction
	 * @throws Exception the exception
	 */
	private void onChannelClosed (Channel channel, Transaction transaction) throws Exception {
		channelTransactionHashes.put(channel.getId(), transaction.getHashAsString());
		openingTransactionHashes.remove(channel.getId());

		boolean clientIssuedTransaction = transaction.getOutput(0).getScriptPubKey().isSentToMultiSig();

		if (clientIssuedTransaction == SideConstants.RUNS_ON_SERVER) {
			Transaction clientTransaction = channel.getChannelTxClient();
			String pubkey1 = Tools.byteToString(transaction.getOutput(0).getScriptPubKey().getChunks().get(1).data);
			String pubkey2 = Tools.byteToString(clientTransaction.getOutput(0).getScriptPubKey().getChunks().get(1).data);
			if (pubkey1.equals(pubkey2)) {
				/**
				 * Transaction uses the same key we used on the last transaction we have on file
				 * We should trust the integrity of our data, but maybe we should do some more checks..
				 *
				 * TODO: Implement further tests, whether the tx is in line with the state of the channel
				 */
				onChannelTransactionBroadcasted(channel, transaction, true, !clientIssuedTransaction);
				return;
			} else {
				/**
				 * The other party broadcasted a revoked channel..
				 */
				onChannelTransactionBroadcasted(channel, transaction, false, !clientIssuedTransaction);
			}

		}

		//		if(transaction.getOutput(0).getScriptPubKey().isSentToMultiSig())
		//
		//
		//		Transaction clientTransaction = channel.getChannelTxServer();

	}

	/**
	 * On channel opened.
	 *
	 * @param channel the channel
	 * @throws SQLException the SQL exception
	 */
	public void onChannelOpened (Channel channel) throws SQLException {
		Transaction tx;
		if (SideConstants.RUNS_ON_SERVER) {
			tx = channel.getRefundTxServer();
		} else {
			tx = channel.getRefundTxClient();
		}

		TransactionTimerTask task = new TransactionTimerTask(peerGroup, tx);
		TransactionTimerTask oldTask = refundTransactionList.put(channel.getId(), task);
		if (oldTask != null) {
			oldTask.cancel();
		}
		timer.schedule(task, new Date(((long) channel.getTimestampRefunds() + 60) * 1000));
	}

	/**
	 * On channel transaction broadcasted.
	 *
	 * @param channel                 the channel
	 * @param transaction             the transaction
	 * @param correct                 the correct
	 * @param serverIssuedTransaction the server issued transaction
	 * @throws Exception the exception
	 */
	public void onChannelTransactionBroadcasted (Channel channel, Transaction transaction, boolean correct, boolean serverIssuedTransaction) throws Exception {
		/**
		 * TODO: While there are some preparations for it already, we only watch for it on the server.
		 * As the server will run 24/7 for now, we can just listen to transactions, while they are broadcasted.
		 *
		 * TODO: On startup, check all transactions that were sent in the meantime..
		 */
		ArrayList<TransactionTimerTask> timerTaskList = new ArrayList<TransactionTimerTask>();

		if (correct) {
			/**
			 * If the channel was closed correctly, we can just look at the list of payments we have in our channel.
			 *
			 * Furthermore, all serverside transactions should be signed with the one key in our database, that is in use, but unexposed.
			 */
			ECKey key = MySQLConnection.getKeyCurrentlyUsed(conn, channel);
			ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
			Transaction channelTransaction;
			if (serverIssuedTransaction) {
				channelTransaction = channel.getChannelTxServer();
			} else {
				channelTransaction = channel.getChannelTxClient();
			}
			/**
			 * If there are payments towards the client, with no secret attached, we have to
			 * 	listen for secrets.
			 */
			boolean listenForSecrets = false;
			for (int i = 0; i < paymentList.size(); ++i) {
				/**
				 * I believe the way updating the channel works, ensures both lists are in the same order.
				 * Come back here if this turns out to be not the case..
				 */
				Payment p = paymentList.get(i);
				TransactionOutput output = channelTransaction.getOutputs().get(i + 2);
				if (p.getSecret() != null) {
					/**
					 * We check now, if the secret has been revealed yet.
					 * TODO: We should come back here, whenever a secret has been revealed related to this channel
					 */
					Transaction settlement;
					if (p.paymentToServer) {
						settlement = p.getSettlementTxSender();
					} else {
						settlement = p.getSettlementTxReceiver();
					}
					ECDSASignature signature1;
					try {
						signature1 = ScriptTools.getSignatureOufOfMultisigInput(settlement.getInput(0));
					} catch (Exception e) {
						signature1 = null;
					}
					ECDSASignature signature2 = Tools.getSignature(settlement, 0, output, key);
					Script inputScript;

					if (p.paymentToServer) {
						/**
						 * Payments towards the server that has been revealed by the receiver
						 *  and should thus be paid to the server aswell..
						 *
						 * If the Server issued this transaction, he is the only one who can claim the settlement.
						 */
						if (serverIssuedTransaction && SideConstants.RUNS_ON_SERVER) {
							continue;
						}
					} else {
						/**
						 * Payments towards the client, that he revealed the secret for.
						 * Usually this should no longer be in the channel, but it might happen.
						 * We pay these out to the client for now, I can't think of a case, where we get here
						 * 	and are allowed to claim these funds..
						 *
						 * If the Client issued this transaction, he is the only one who can claim the settlement.
						 */
						if (!serverIssuedTransaction && SideConstants.RUNS_ON_SERVER) {
							continue;
						}
					}
					if (SideConstants.RUNS_ON_SERVER) {
						inputScript = ScriptTools.getSettlementScriptSig(channel, signature1, signature2, p.getSecret(), serverIssuedTransaction, p
								.paymentToServer);
					} else {
						inputScript = ScriptTools.getSettlementScriptSig(channel, signature2, signature1, p.getSecret(), serverIssuedTransaction, p
								.paymentToServer);
					}
					/**
					 * Settlements have no locktime, therefore we should broadcast them straight-away.
					 * TODO: This might be a problem if we come back here very often..
					 */
					settlement.getInput(0).setScriptSig(inputScript);
					peerGroup.broadcastTransaction(settlement);

				} else {
					/**
					 * Payments that do not have a secret attached to it (yet) that should therefore
					 * 	be scheduled for refunds. As we come back here later, the list of refunds
					 * 	will likely change.
					 * In any case, if we hit the timestamp of the refund tx, it is always fine to try
					 * 	to broadcast it.
					 */
					Transaction refund;
					if (p.paymentToServer) {
						refund = p.getRefundTxSender();
					} else {
						listenForSecrets = true;
						refund = p.getRefundTxReceiver();
					}
					ECDSASignature signature1;
					try {
						signature1 = ScriptTools.getSignatureOufOfMultisigInput(refund.getInput(0));
					} catch (Exception e) {
						signature1 = null;
					}
					ECDSASignature signature2 = Tools.getSignature(refund, 0, output, key);
					Script inputScript;

					if (SideConstants.RUNS_ON_SERVER) {
						inputScript = ScriptTools.getRefundScriptSig(channel, signature1, signature2, serverIssuedTransaction, p.paymentToServer);
					} else {
						inputScript = ScriptTools.getRefundScriptSig(channel, signature2, signature1, serverIssuedTransaction, p.paymentToServer);
					}

					refund.getInput(0).setScriptSig(inputScript);

					TransactionTimerTask task = new TransactionTimerTask(peerGroup, refund);
					timerTaskList.add(task);
					timer.schedule(task, new Date((refund.getLockTime() - 60) * 1000));
				}
			}
			if (listenForSecrets) {
				channelTransactionHashes.put(channel.getId(), transaction.getHashAsString());
			}
		} else {
			/**
			 * This channel transaction was revoked. We need to find the key (brute force) and claim
			 * 	as many funds as possible, as fast as possible.
			 *
			 * We assume that only the other party cheats.
			 */
			String masterKey;
			String pubKey;
			ECKey ourKey;
			Address ourAddress;
			if (SideConstants.RUNS_ON_SERVER) {
				masterKey = channel.getMasterPrivateKeyClient();
				pubKey = Tools.byteToString(transaction.getOutput(0).getScriptPubKey().getChunks().get(1).data);
				ourKey = channel.getServerKeyOnServer();
				ourAddress = channel.getChangeAddressServerAsAddress();
			} else {
				masterKey = channel.getMasterPrivateKeyServer();
				pubKey = Tools.byteToString(transaction.getOutput(1).getScriptPubKey().getChunks().get(2).data);
				ourKey = channel.getClientKeyOnClient();
				ourAddress = channel.getChangeAddressClientAsAddress();
			}
			ECKey key = MySQLConnection.getKey(conn, pubKey);
			if (key == null) {
				/**
				 * We don't have the private key in our database anymore, so we need to reconstruct it from the masterkey..
				 */
				key = HashDerivation.bruteForceKey(masterKey, pubKey);
				if (key == null) {
					System.out.println("Can't brute force.. :(");
					return;
				}
			}

			/**
			 * Just try to claim each output of the channel transaction..
			 */
			System.out.println("Revoked Transaction: ");
			System.out.println(transaction);
			System.out.println("Managed to brute force the private key of the other party..");

			for (int i = 0; i < transaction.getOutputs().size(); ++i) {
				TransactionOutput output = transaction.getOutput(i);

				Transaction t = new Transaction(Constants.getNetwork());
				t.addOutput(Coin.valueOf(output.getValue().value - Tools.getTransactionFees(1, 2)), ourAddress);
				t.addInput(transaction.getHash(), i, Tools.getDummyScript());

				ECDSASignature signature1 = Tools.getSignature(t, 0, output, ourKey);
				ECDSASignature signature2 = Tools.getSignature(t, 0, output, key);

				if (SideConstants.RUNS_ON_SERVER) {
					t.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(signature2, signature1));
				} else {
					t.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(signature1, signature2));
				}
				/**
				 * Validate locally, if the transaction signs correctly
				 */
				try {
					t.getInput(0).getScriptSig().correctlySpends(t, 0, output.getScriptPubKey());
					peerGroup.broadcastTransaction(t).broadcast();
					System.out.println("Claiming " + output);
					continue;
				} catch (ScriptException e) {
				}

				/**
				 * Also, in case it is a clientside payment to client,
				 * 	test our signature only..
				 */

				t.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(signature1));
				/**
				 * Validate locally, if the transaction signs correctly
				 */
				try {
					t.getInput(0).getScriptSig().correctlySpends(t, 0, output.getScriptPubKey());
					peerGroup.broadcastTransaction(t).broadcast();
					System.out.println("Claiming " + output);
				} catch (ScriptException e) {
				}

			}

		}
		/**
		 * Replace the old scheduled task, and make sure they don't get executed..
		 */
		ArrayList<TransactionTimerTask> oldTimerTaskList = additionalTransactionList.put(channel.getId(), timerTaskList);
		if (oldTimerTaskList != null) {
			for (TransactionTimerTask task : oldTimerTaskList) {
				task.cancel();
			}
		}

	}

	/**
	 * On confidence changed.
	 *
	 * @param transaction the transaction
	 * @throws Exception the exception
	 */
	public void onConfidenceChanged (Transaction transaction) throws Exception {
		/**
		 * Check confidence for our opening transaction
		 */
		TransactionConfidence confidence = transaction.getConfidence();
		BiMap<String, Integer> a = openingTransactionHashesOfFreshChannel.inverse();
		Integer id = openingTransactionHashesOfFreshChannel.inverse().get(transaction.getHashAsString());
		if (confidence.getDepthInBlocks() >= Constants.MIN_CONFIRMATION_TIME_FOR_CHANNEL) {

			if (id != null) {
				Channel channel = MySQLConnection.getChannel(conn, id);
				openingTransactionHashes.put(channel.getId(), channel.getOpeningTxHash());
				openingTransactionHashesOfFreshChannel.remove(channel.getId());
				channel.setReady(true);
				channel.setEstablishPhase(0);
				MySQLConnection.updateChannel(conn, channel);
				return;
			}
		} else if (confidence.getConfidenceType() == ConfidenceType.DEAD) {
			//TODO: Remove this transaction and the channel, somehow it got overwritten..
		}
	}

	/**
	 * On transaction.
	 *
	 * @param transaction the transaction
	 * @throws Exception the exception
	 */
	public void onTransaction (Transaction transaction) throws Exception {
		for (TransactionInput input : transaction.getInputs()) {
			/**
			 * Check for a channel transaction. These only pay the first output of our opening tx.
			 */
			if (input.getOutpoint().getIndex() == 0) {

				Integer id = openingTransactionHashes.inverse().get(input.getOutpoint().getHash().toString());
				if (id != null) {

					Channel c = MySQLConnection.getChannel(conn, id);
					//TODO: Somehow the channel got closed, find out how (correctly?) and act accordingly..
					onChannelClosed(c, transaction);
					return;
				}
				/**
				 * Check for a transaction that uses a secret to claim a payment.
				 */
			} else if (input.getOutpoint().getIndex() >= 2) {
				Integer id = channelTransactionHashes.inverse().get(input.getOutpoint().getHash().toString());
				if (id != null) {
					/**
					 * Check if a payment was used
					 */
					if (input.getScriptSig().getChunks().get(0).data.length == 20) {
						String secret = Tools.byteToString(input.getScriptSig().getChunks().get(0).data);
						String secretHash = Tools.hashSecret(input.getScriptSig().getChunks().get(0).data);
						MySQLConnection.updatePayment(conn, secretHash, secret);
						if (!MySQLConnection.checkForSecretsInChannel(conn, id)) {
							/**
							 * There are no payments towards the client left anymore.
							 * We can stop watching out for transactions for this channel.
							 */
							channelTransactionHashes.remove(id);
						}
					}

					Channel c = MySQLConnection.getChannel(conn, id);
					//TODO: Somehow the channel got closed, find out how (correctly?) and act accordingly..
					onChannelClosed(c, transaction);
					return;
				}
			}
		}
	}

	/**
	 * Rebroadcast opening transactions.
	 *
	 * @param peer the peer
	 * @throws NotYetConnectedException the not yet connected exception
	 * @throws SQLException             the SQL exception
	 */
	public void rebroadcastOpeningTransactions (Peer peer) throws NotYetConnectedException, SQLException {
		for (String hash : openingTransactionHashesOfFreshChannel.values()) {
			peer.sendMessage(MySQLConnection.getTransaction(conn, hash));
		}
	}

}
